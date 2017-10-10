package hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

public class Step1 {
	public static class Mapper1 extends Mapper<LongWritable,Text,Text,Text>
	{
		private Text outKey = new Text();
		private Text outValue = new Text();
		
		/*
			待转置矩阵
			0	3	-1	2	-3
			1	3	5	-2	-1
			0	1	4	-1	2
			-2	2	-1	1	2
		*/
		/*
			目标矩阵
			0	1	1	-2
			3	3	1	2
			-1	5	4	-1
			2	-2	-1	1
			-3	-1	2	2
		*/
		//对于每一行，以第一行为例
		//key : 1
		//value : "1	1_0,2_3,3_-1,4_2,5_-3"
		@Override
		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {
			String[] rowAndline = value.toString().split("\t");
			//rowAndline : {"1","1_0,2_3,3_-1,4_2,5_-3"}
			String row = rowAndline[0];
			//row "1"
			String[] lines = rowAndline[1].split(",");
			//rowAndline[1] : "1_0,2_3,3_-1,4_2,5_-3"
			//lines : {"1_0","2_3","3_-1","4_2","5_-3"}
			for(String line : lines)//对于每一列，以第一列为例，line "1_0"
			{
				String colunm = line.split("_")[0];
				//colunm : 1
				String valueStr = line.split("_")[1];
				//valueStr : 0 
				outKey.set(colunm);
				//将列作为行
				outValue.set(row + "_" + valueStr);
				//将行作为列
				context.write(outKey, outValue);
				// 产生(1,"1_0")
			}
			//循环结束，对于{"1_0","2_3","3_-1","4_2","5_-3"}
			//产生(1,"1_0") 第一行，第一列_0    (2,"1_3")  第二行，第一列_3		(3,"1_-1") (4,"1_2")(5,"1_-3")
			/*
			目标转置矩阵
			0	1	1	-2
			3	3	1	2
			-1	5	4	-1
			2	-2	-1	1
			-3	-1	2	2
			*/
			//正好对应于转置矩阵的第一列
		}
		/*
			所有map操作产生
			 ("1","1_0")	("2","1_3") 	("3","1_-1")	("4","1_2")		("5","1_-3")
			（"1","2_1"）	("2","2_3") 	("3","2_5")	    ("4","2_-2")	("5","2_-1")
			（"1","3_0"）	("2","3_1")	    ("3","3_4")		("4","3_-1")	("5","3_2")
			（"1","4_-2"）  ("2","4_2")	    ("3","4_-1")	("4","4_1")		("5","4_2")
		*/

	}
	

	/*
		Reduce任务，将map操作产生的所有键值对集合进行合并，生成转置矩阵的存储表示
		key值相同的值会组成值的集合
		如：
		key:"1"时
		values:{"3_0","1_0","4_-2","2_1"} 
		注意：这里就是为什么要进行列标号的原因，values的顺序不一定就是原来矩阵列的顺序
	*/
	
	public static class Reducer1 extends Reducer<Text,Text,Text,Text>
	{
		private Text outKey = new Text();
		private Text outValue = new Text();
		
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			
			StringBuilder sb = new StringBuilder();
			for(Text text : values)
			{
				sb.append(text + ",");
			}
			//sb : "3_0,1_0,4_-2,2_1,"
			//注意这里末尾有个逗号
			String line = "";
			if(sb.toString().endsWith(","))
			{
				line = sb.substring(0,sb.length()-1);
			}
			//去掉逗号
			//line : "3_0,1_0,4_-2,2_1"
			outKey.set(key);
			outValue.set(line);
			//("1","3_0,1_0,4_-2,2_1")
			context.write(outKey, outValue);
		}
		
	}
	
	private static final String INPATH = "input/matrix.txt";//输入文件路径
	private static final String OUTPATH = "output/step1";//输出文件路径
	private static final String HDFS = "hdfs://pc1:9000";//HDFS路径
	
	public void run() throws IOException, ClassNotFoundException, InterruptedException {
		 Configuration conf = new Configuration();
		    //String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		    //String[] otherArgs = {"hdfs://pc1:9000/input/chenjie.txt","hdfs://pc1:9000/output/out4"};
		    String[] otherArgs = {"input/matrix.txt","hdfs://pc1:9000/output/step1_2"};
		    //这里需要配置参数即输入和输出的HDFS的文件路径
		    if (otherArgs.length != 2) {
		      System.err.println("Usage: wordcount <in> <out>");
		      System.exit(2);
		    }
		    //conf.set("fs.defaultFS",HDFS);
		   // JobConf conf1 = new JobConf(WordCount.class);
		    Job job = new Job(conf, "step1");//Job(Configuration conf, String jobName) 设置job名称和
		    job.setJarByClass(Step1.class);
		    job.setMapperClass(Mapper1.class); //为job设置Mapper类 
		    //job.setCombinerClass(IntSumReducer.class); //为job设置Combiner类  
		    job.setReducerClass(Reducer1.class); //为job设置Reduce类 

		    job.setMapOutputKeyClass(Text.class);  
		    job.setMapOutputValueClass(Text.class); 

		    job.setOutputKeyClass(Text.class);        //设置输出key的类型
		    job.setOutputValueClass(Text.class);//  设置输出value的类型

		    job.setOutputFormatClass(SequenceFileOutputFormat.class);
		    FileInputFormat.addInputPath(job, new Path(otherArgs[0])); //为map-reduce任务设置InputFormat实现类   设置输入路径

		    FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));//为map-reduce任务设置OutputFormat实现类  设置输出路径
		    System.exit(job.waitForCompletion(true) ? 0 : 1);
		
		
		/*Configuration conf = new Configuration();
		conf.set("fs.defaultFS",HDFS);
		Job job = Job.getInstance(conf,"step1");
		job.setJarByClass(Step1.class);
		job.setMapperClass(Mapper1.class);
		job.setReducerClass(Reducer1.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileSystem fs = FileSystem.get(conf);
		Path inPath = new Path(INPATH);
		if(fs.exists(inPath))
		{
			//FileInputFormat.addInputPath(conf, inPath);
		}
		Path outPath = new Path(OUTPATH);
		if(fs.exists(outPath))
		{
			fs.delete(outPath, true);
		}*/
		
	}
	
	public static void main(String[] args)
	{
		try {
			new Step1().run();
		} catch (ClassNotFoundException | IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
