package com.alibaba.alink;

import com.alibaba.alink.common.io.filesystem.BaseFileSystem;
import com.alibaba.alink.common.io.filesystem.FilePath;
import com.alibaba.alink.common.io.filesystem.HadoopFileSystem;
import com.alibaba.alink.operator.batch.BatchOperator;
import com.alibaba.alink.operator.batch.dataproc.SplitBatchOp;
import com.alibaba.alink.operator.batch.recommendation.AlsRateRecommBatchOp;
import com.alibaba.alink.operator.batch.recommendation.AlsTrainBatchOp;
import com.alibaba.alink.operator.batch.sink.AkSinkBatchOp;
import com.alibaba.alink.operator.batch.source.CsvSourceBatchOp;

/**
 *  * Example for ALS.
 *   */
public class ALSExample {

    public static void main(String[] args) throws Exception {

        String url = "https://alink-release.oss-cn-beijing.aliyuncs.com/data-files/movielens_ratings.csv";
        String schema = "userid bigint, movieid bigint, rating double, timestamp string";

        BatchOperator data = new CsvSourceBatchOp()
            .setFilePath(url).setSchemaStr(schema);

        SplitBatchOp spliter = new SplitBatchOp().setFraction(0.8);
        spliter.linkFrom(data);

        BatchOperator trainData = spliter;
        BatchOperator testData = spliter.getSideOutput(0);

        AlsTrainBatchOp als = new AlsTrainBatchOp()
            .setUserCol("userid").setItemCol("movieid").setRateCol("rating")
            .setNumIter(10).setRank(10).setLambda(0.1);

        BatchOperator model = als.linkFrom(trainData);
        AlsRateRecommBatchOp predictor = new AlsRateRecommBatchOp()
            .setUserCol("userid").setItemCol("movieid").setRecommCol("prediction_result");
        BatchOperator preditionResult = predictor.linkFrom(model, testData).select("rating, prediction_result");

        BaseFileSystem<?> hadoopFileSystem = new HadoopFileSystem("3.2.2", "hdfs://10.10.12.141:9000");
        AkSinkBatchOp akSinkToHdfs = new AkSinkBatchOp()
                .setFilePath(new FilePath("/tmp/alinkre1", hadoopFileSystem))
                .setOverwriteSink(true);
        preditionResult.link(akSinkToHdfs);
        BatchOperator.execute();
    }
}

