package com.org.xidian;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public class MapReduceAvgGradeStudent {

    //自定义mapper，继承org.apache.hadoop.mapreduce.Mapper
    public static class MyMapper extends org.apache.hadoop.mapreduce.Mapper<LongWritable, Text, Text, Text>{
        // Mapper<LongWritable, Text, Text, Text>后两个参数类型和context要生成的kv对的数据类型一致
        @Override
        protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, Text>.Context context)
                throws IOException, InterruptedException {
            String line = value.toString();
            // split函数用于按指定字符（串）或正则去分割某个字符串，结果以字符串数组形式返回
            String[] splited = line.split(",");
            // foreach 就是 for（元素类型t 元素变量x:遍历对象obj）{引用x的java语句}
            // 一行：班级,姓名,课程名,性质,分数
            // for (String word : splited) {
            //     context.write(new Text(word), new LongWritable(1));
            // }
            if(splited[3].equals("必修")){  // 过滤掉有选修课程的行
                String studentname = splited[1];
                String grade = splited[4];
                context.write(new Text(studentname), new Text(grade));
                // <‘xxx’: '90'> <‘xxx’: '80'> <‘yyy’: '86'> <‘yyy’: '82'>...
            }
        }
    }

    public static class MyReducer extends org.apache.hadoop.mapreduce.Reducer<Text, Text, Text, Text>{
        @Override
        protected void reduce(Text key, Iterable<Text> grades, Reducer<Text, Text, Text, Text>.Context context) throws IOException, InterruptedException {

            Double sum = 0.0;
            int i = 0;
            for (Text grade : grades) {
                // 先转为String再转为Int，便于计算
                sum += Double.valueOf(grade.toString());
                i += 1;
            }
            Double avg = sum / i;
            String avg_grade = String.format("%.2f", avg);
            context.write(key, new Text(avg_grade));

        }
    }

    //客户端代码，写完交给ResourceManager框架去执行
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, MapReduceAvgGradeStudent.class.getSimpleName());
        //打成jar执行
        job.setJarByClass(MapReduceAvgGradeStudent.class);

        //数据在哪里
        FileInputFormat.setInputPaths(job, args[0]);
        
        //使用哪个mapper处理输入的数据
        job.setMapperClass(MyMapper.class);

        //map输出的数据类型是什么
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        //使用哪个reducer处理输入的数据
        job.setReducerClass(MyReducer.class);

        //reduce输出的数据类型是什么
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        //数据输出到哪里
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        //交给yarn去执行，直到执行结束才退出本程序
        job.waitForCompletion(true);
    }
}