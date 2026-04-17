package com.example.mictestprogram.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TestResultDao_Impl implements TestResultDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TestResultEntity> __insertionAdapterOfTestResultEntity;

  public TestResultDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTestResultEntity = new EntityInsertionAdapter<TestResultEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `test_results` (`id`,`testDateMillis`,`totalCount`,`correctCount`,`accuracy`,`details`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TestResultEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTestDateMillis());
        statement.bindLong(3, entity.getTotalCount());
        statement.bindLong(4, entity.getCorrectCount());
        statement.bindDouble(5, entity.getAccuracy());
        statement.bindString(6, entity.getDetails());
      }
    };
  }

  @Override
  public Object insert(final TestResultEntity result,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTestResultEntity.insert(result);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TestResultEntity>> observeAll() {
    final String _sql = "SELECT * FROM test_results ORDER BY testDateMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"test_results"}, new Callable<List<TestResultEntity>>() {
      @Override
      @NonNull
      public List<TestResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTestDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "testDateMillis");
          final int _cursorIndexOfTotalCount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCount");
          final int _cursorIndexOfCorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "correctCount");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfDetails = CursorUtil.getColumnIndexOrThrow(_cursor, "details");
          final List<TestResultEntity> _result = new ArrayList<TestResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TestResultEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTestDateMillis;
            _tmpTestDateMillis = _cursor.getLong(_cursorIndexOfTestDateMillis);
            final int _tmpTotalCount;
            _tmpTotalCount = _cursor.getInt(_cursorIndexOfTotalCount);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final double _tmpAccuracy;
            _tmpAccuracy = _cursor.getDouble(_cursorIndexOfAccuracy);
            final String _tmpDetails;
            _tmpDetails = _cursor.getString(_cursorIndexOfDetails);
            _item = new TestResultEntity(_tmpId,_tmpTestDateMillis,_tmpTotalCount,_tmpCorrectCount,_tmpAccuracy,_tmpDetails);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
