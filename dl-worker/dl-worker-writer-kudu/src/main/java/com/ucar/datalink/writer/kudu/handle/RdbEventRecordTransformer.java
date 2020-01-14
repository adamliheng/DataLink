package com.ucar.datalink.writer.kudu.handle;

import com.ucar.datalink.contract.log.rdbms.RdbEventRecord;
import com.ucar.datalink.domain.media.MediaMappingInfo;
import com.ucar.datalink.worker.api.task.TaskWriterContext;
import com.ucar.datalink.worker.api.transform.BuiltInRdbEventRecordTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by lubiao on 2017/6/28.
 */
public class RdbEventRecordTransformer extends BuiltInRdbEventRecordTransformer {

    private static final Logger logger = LoggerFactory.getLogger(RdbEventRecordHandler.class);

    @Override
    protected RdbEventRecord transformOne(RdbEventRecord record, MediaMappingInfo mappingInfo, TaskWriterContext context) {
        //在执行父类的方法之前，先拿到原始的表名，然后构造列名前缀
        String tableName = record.getTableName();
        RdbEventRecord result = super.transformOne(record, mappingInfo, context);
        if (result != null) {
            result.metaData().put(Constants.ORIGIN_TABLE_NAME, tableName);
        }
        return result;
    }
}
