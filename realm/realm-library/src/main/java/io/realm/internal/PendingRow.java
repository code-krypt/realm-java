package io.realm.internal;

import java.lang.ref.WeakReference;
import java.util.Date;

import io.realm.RealmChangeListener;
import io.realm.RealmFieldType;

/**
 * A PendingRow is a row relies on a pending async query.
 * Before the query returns, calling any accessors will immediately throw. In this case run {@link #executeQuery()} to
 * get the queried row immediately. If the query results is empty, an {@link InvalidRow} will be returned.
 * After the query returns, {@link FrontEnd#onQueryFinished(Row)} will be called to give the front end a chance to reset
 * the row. If the async query returns an empty result, the query will be executed again later until a valid row is
 * contained by the query results.
 */
public class PendingRow implements Row {

    // Implement this interface to reset the PendingRow to a Row backed by real data when query returned.
    public interface FrontEnd {
        // When asyncQuery is true, the pending query is executed asynchronously.
        void onQueryFinished(Row row);
    }

    private static final String QUERY_NOT_RETURNED_MESSAGE =
            "The pending query has not been executed.";
    private static final String PROXY_NOT_SET_MESSAGE = "The 'frontEnd' has not been set.";
    private static final String QUERY_EXECUTED_MESSAGE =
            "The query has been executed. This 'PendingRow' is not valid anymore.";

    private Collection pendingCollection;
    private RealmChangeListener<PendingRow> listener;
    private WeakReference<FrontEnd> frontEndRef;
    private boolean returnCheckedRow;

    public PendingRow(SharedRealm sharedRealm, TableQuery query, SortDescriptor sortDescriptor,
                      final boolean returnCheckedRow) {
        pendingCollection = new Collection(sharedRealm, query, sortDescriptor, null);

        listener = new RealmChangeListener<PendingRow>() {
            @Override
            public void onChange(PendingRow pendingRow) {
                if (frontEndRef == null) {
                    throw new IllegalStateException(PROXY_NOT_SET_MESSAGE);
                }
                FrontEnd frontEnd = frontEndRef.get();
                if (frontEnd == null) {
                    // The front end is GCed.
                    clearPendingCollection();
                    return;
                }

                if (pendingCollection.isValid()) {
                    // PendingRow will always get the first Row of the query since we only support findFirst.
                    UncheckedRow uncheckedRow = pendingCollection.firstUncheckedRow();
                    // If no rows returned by the query, just wait for the query updates until it returns a valid row.
                    if (uncheckedRow != null) {
                        Row row = returnCheckedRow ? CheckedRow.getFromRow(uncheckedRow) : uncheckedRow;
                        // Ask the front end to reset the row and stop async query.
                        frontEnd.onQueryFinished(row);
                        clearPendingCollection();
                    }
                } else {
                    // The Realm is closed. Do nothing then.
                    clearPendingCollection();
                }
            }
        };
        pendingCollection.addListener(this, listener);
        this.returnCheckedRow = returnCheckedRow;
    }

    // To set the front end of this PendingRow.
    public void setFrontEnd(FrontEnd frontEnd) {
        this.frontEndRef = new WeakReference<FrontEnd>(frontEnd);
    }

    @Override
    public long getColumnCount() {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public String getColumnName(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public long getColumnIndex(String columnName) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public RealmFieldType getColumnType(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public Table getTable() {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public long getIndex() {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public long getLong(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public boolean getBoolean(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public float getFloat(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public double getDouble(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public Date getDate(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public String getString(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public byte[] getBinaryByteArray(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public long getLink(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public boolean isNullLink(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public LinkView getLinkList(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public void setLong(long columnIndex, long value) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public void setBoolean(long columnIndex, boolean value) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public void setFloat(long columnIndex, float value) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public void setDouble(long columnIndex, double value) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public void setDate(long columnIndex, Date date) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public void setString(long columnIndex, String value) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public void setBinaryByteArray(long columnIndex, byte[] data) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public void setLink(long columnIndex, long value) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public void nullifyLink(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public boolean isNull(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public void setNull(long columnIndex) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    @Override
    public boolean isAttached() {
        return false;
    }

    @Override
    public boolean hasColumn(String fieldName) {
        throw new IllegalStateException(QUERY_NOT_RETURNED_MESSAGE);
    }

    private void clearPendingCollection() {
        pendingCollection.removeListener(this, listener);
        pendingCollection = null;
        listener = null;
    }

    public Row executeQuery() {
        if (pendingCollection == null) {
            throw new IllegalStateException(QUERY_EXECUTED_MESSAGE);
        }
        if (frontEndRef == null) {
            throw new IllegalStateException(PROXY_NOT_SET_MESSAGE);
        }

        UncheckedRow uncheckedRow = pendingCollection.firstUncheckedRow();
        clearPendingCollection();

        if (uncheckedRow == null) {
            return InvalidRow.INSTANCE;
        }
        return returnCheckedRow ? CheckedRow.getFromRow(uncheckedRow) : uncheckedRow;
    }
}
