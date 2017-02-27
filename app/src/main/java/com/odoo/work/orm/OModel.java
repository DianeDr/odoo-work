package com.odoo.work.orm;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.utils.ODateUtils;
import com.odoo.work.R;
import com.odoo.work.orm.data.ListRow;
import com.odoo.work.orm.models.IrModel;
import com.odoo.work.orm.models.LocalRecordState;
import com.odoo.work.orm.models.ModelRegistry;
import com.odoo.work.orm.sync.SyncAdapter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class OModel extends SQLiteOpenHelper implements BaseColumns {
    public static final String TAG = OModel.class.getSimpleName();
    private static final String DB_NAME = "OdooWork";
    private static final int DB_VERSION = 1;
    public static final int INVALID_ROW_ID = -1;

    OColumn _id = new OColumn("Local Id", ColumnType.INTEGER).makePrimaryKey()
            .makeAutoIncrement().makeLocal();
    OColumn id = new OColumn("Server Id", ColumnType.INTEGER);
    OColumn write_date = new OColumn("Write date", ColumnType.DATETIME).makeLocal().setDefaultValue("false");
    OColumn is_dirty = new OColumn("Is dirty", ColumnType.BOOLEAN).makeLocal().setDefaultValue("false");
    private String mModelName;
    private Context mContext;

    public OModel(Context context, String model) {
        super(context, DB_NAME, null, DB_VERSION);
        mModelName = model;
        mContext = context;
    }

    public static OModel createInstance(String modelName, Context mContext) {

        HashMap<String, OModel> models = new ModelRegistry().models(mContext);
        for (String key : models.keySet()) {
            OModel model = models.get(key);
            if (model.getModelName().equals(modelName)) {
                return model;
            }
        }
        return null;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        HashMap<String, OModel> map = new ModelRegistry().models(mContext);
        for (OModel model : map.values()) {
            CreateQueryBuilder statementBuilder = new CreateQueryBuilder(model);
            String sql = statementBuilder.createQuery();
            if (sql != null) {
                db.execSQL(sql);
                Log.d(TAG, "Table created: " + model.getTableName());
            }
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }

    public String getTableName() {
        return mModelName.replace(".", "_");
    }

    public String getModelName() {
        return mModelName;
    }

    public String getAuthority() {
        return mContext.getString(R.string.main_authority);
    }

    public Uri getUri() {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(getAuthority());
        uriBuilder.appendPath("data");
        uriBuilder.scheme("content");
        uriBuilder.appendQueryParameter("model", getModelName());
        return uriBuilder.build();
    }

    public List<OColumn> getColumns() {
        List<OColumn> columnList = new ArrayList<>();
        List<Field> fieldList = new ArrayList<>();

        fieldList.addAll(Arrays.asList(getClass().getSuperclass().getDeclaredFields()));
        fieldList.addAll(Arrays.asList(getClass().getDeclaredFields()));

        for (Field field : fieldList) {
            field.setAccessible(true);
            if (field.getType().isAssignableFrom(OColumn.class)) {
                try {
                    OColumn column = (OColumn) field.get(this);
                    column.name = field.getName();
                    columnList.add(column);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return columnList;
    }

    public int create(ContentValues values) {
        Long id = null;
        if (getUri() != null) {
            Uri uri = mContext.getContentResolver().insert(getUri(), values);
            return Integer.parseInt(uri.getLastPathSegment());
        } else {
            SQLiteDatabase database = getWritableDatabase();
            id = database.insert(getTableName(), null, values);
            database.close();
            return id.intValue();
        }
    }

    public int update(ContentValues values, String where, String... args) {
        SQLiteDatabase database = getReadableDatabase();
        int id = database.update(getTableName(), values, where, args);
        database.close();
        return id;
    }

    public void deleteAll() {
        delete(null);
    }

    public int delete(String where, String... args) {
        LocalRecordState recordState = new LocalRecordState(mContext);
        List<Integer> serverIds = selectServerIds(where, args);
        recordState.addDeleted(getModelName(), serverIds);

        SQLiteDatabase database = getWritableDatabase();
        int id = database.delete(getTableName(), where, args);
        database.close();
        return id;
    }

    public int delete(int row_id) {
        return delete("_id = ?", row_id + "");
    }

    public int count() {
        int count = 0;
        SQLiteDatabase database = getReadableDatabase();

        Cursor cursor = database.rawQuery("SELECT COUNT(*) AS TOTAL FROM " + getTableName(), null);

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        database.close();
        return count;
    }

    public List<ListRow> select() {
        return select(null);
    }

    public int selectRowId(int server_id) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(getTableName(), new String[]{"_id"}, "id = ? ",
                new String[]{server_id + ""}, null, null, null);
        int row_id = INVALID_ROW_ID;
        if (cursor.moveToFirst()) {
            row_id = cursor.getInt(0);
        }
        database.close();
        cursor.close();
        return row_id;
    }

    public List<Integer> selectServerIds(String where, String... args) {
        List<Integer> ids = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(getTableName(), new String[]{"id"}, where, args,
                null, null, null);
        if (cursor.moveToFirst()) {
            do {
                ids.add(cursor.getInt(0));
            } while (cursor.moveToNext());
        }
        database.close();
        cursor.close();
        return ids;
    }

    public List<ListRow> select(String where, String... args) {
        List<ListRow> rows = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        args = args.length > 0 ? args : null;
        Cursor cursor = database.query(getTableName(), null, where, args, null, null, "_id DESC");
        if (cursor.moveToFirst()) {
            do {
                rows.add(new ListRow(cursor));
            } while (cursor.moveToNext());
        }

        database.close();
        cursor.close();
        return rows;
    }

    public String[] getServerColumns() {
        List<String> serverColumns = new ArrayList<>();
        for (OColumn column : getColumns()) {
            if (!column.isLocal) {
                serverColumns.add(column.name);
            }
        }
        serverColumns.add("write_date");
        return serverColumns.toArray(new String[serverColumns.size()]);
    }

    public int updateOrCreate(ContentValues values, String where, String... args) {
        List<ListRow> records = select(where, args);
        if (records.size() > 0) {
            ListRow row = records.get(0);
            update(values, where, args);
            return row.getInt(_ID);
        } else {
            create(values);
        }
        return 0;
    }

    public ContentProviderResult[] batchInsert(List<ContentValues> values) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        for (ContentValues value : values) {
            operations.add(ContentProviderOperation.newInsert(getUri())
                    .withValues(value).withYieldAllowed(true).build());
        }
        try {
            return mContext.getContentResolver().applyBatch(getAuthority(), operations);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void batchUpdate(List<ContentValues> values) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        for (ContentValues value : values) {
            operations.add(ContentProviderOperation
                    .newUpdate(Uri.withAppendedPath(getUri(), value.get("_id") + ""))
                    .withValues(value)
                    .withYieldAllowed(true)
                    .build());
        }
        try {
            mContext.getContentResolver().applyBatch(getAuthority(), operations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public OModel createModel(String modelName) {
        return ModelRegistry.getModel(mContext, modelName);
    }

    /**
     * Sets last sync date to current date time
     */
    public void updateLastSyncDate() {
        IrModel model = new IrModel(mContext);
        ContentValues values = new ContentValues();
        values.put("model", getModelName());
        values.put("last_sync_on", ODateUtils.getUTCDateTime());
        model.updateOrCreate(values, "model = ?", getModelName());
    }

    public String getLastSyncDate() {
        IrModel model = new IrModel(mContext);
        List<ListRow> items = model.select("model = ?", getModelName());
        if (!items.isEmpty()) {
            return items.get(0).getString("last_sync_on");
        }
        return null;
    }

    public List<Integer> getServerIds() {
        List<Integer> ids = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(getTableName(), new String[]{"id"}, "id != ?", new String[]{"0"}, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                ids.add(cursor.getInt(0));
            } while (cursor.moveToNext());
        }
        database.close();
        cursor.close();
        return ids;
    }

    public ODomain syncDomain() {
        return new ODomain();
    }

    public boolean isEmpty() {
        return count() <= 0;
    }

    public SyncAdapter getSyncAdapter() {
        return new SyncAdapter(mContext, true, this);
    }

    public Context getContext() {
        return mContext;
    }
}
