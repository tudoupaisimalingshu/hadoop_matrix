package hadoop;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import hadoop.Step1.Mapper1;
import hadoop.Step1.Reducer1;

public class Step2 {
	public static class Mapper2 extends Mapper<LongWritable,Text,Text,Text>
	{
		private Text outKey = new Text();
		private Text outValue = new Text();
		private List<String> cacheList = new ArrayList<String>();
		
		
		/***
		 * 	将保存右侧矩阵的文件缓存到内存中，每一行为一个字符串，是所有行构成list
		 */
		@Override
		protected void setup(Context context)
				throws IOException, InterruptedException {
			FileReader fr = new FileReader("matrix2");
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while((line = br.readLine()) != null)
			{
				cacheList.add(line);
			}
			fr.close();
			br.close();
		}
		
		
		/*	左侧矩阵逻辑形式
		 * 1	2	-2	0
		 * 3	3	4	-3
		 * -2	0	2	3
		 * 5	3	-1	2
		 * -4	2	0	2
		 * 左侧矩阵物理形式
		 * 1	1_1,2_2,3_-2,4_0
		 * 2	1_3,2_3,3_4,4_-3
		 * 3	1_-2,2_0,3_2,4_3
		 * 4	1_5,2_3,3_-1,4_2
		 * 5	1_-4,2_2,3_0,4_2
		 * 
		 * 右侧矩阵（已转置）物理形式
		 *  1	3_0,1_0,4_-2,2_1
			2	3_1,4_2,2_3,1_3
			3	4_-1,1_-1,3_4,2_5
			4	1_2,3_-1,4_1,2_-2
			5	4_2,3_2,1_-3,2_-1
			
			key: "1"
			value: "1	1_1,2_2,3_-2,4_0"
		 * */
		@Override
		protected void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String[] rowAndline = value.toString().split("\t");
			//获得行号
			//rowAndline : {"1","1_1,2_2,3_-2,4_0"}
			String row_matrix1 = rowAndline[0];
			//row_matrix1 ："1"
			String[] column_value_array_matrix1 = rowAndline[1].split(",");
			//获得各列
			//rowAndline[1] ： "1_1,2_2,3_-2,4_0"
			//column_value_array_matrix1 : {"1_1","2_2","3_-2","4_0"}
			for(String line : cacheList)// 以line:"3		4_-1,1_-1,3_4,2_5"为例
			{
				String[] rowAndline2 = line.toString().split("\t");
				//rowAndline2 : {"3","4_-1,1_-1,3_4,2_5"}
				String row_matrix2 = rowAndline2[0];
				//获得转置矩阵line行的行号（原右矩阵的列号）
				String[] column_value_array_matrix2 = rowAndline2[1].split(",");
				//rowAndline2[1] : "4_-1,1_-1,3_4,2_5"
				//column_value_array_matrix2 : {"4_-1","1,-1","3_4","2_5"}
				int result = 0;
				//保存成绩累加结果
				for(String column_value_matrix1 : column_value_array_matrix1)//对于左侧矩阵line行的每一列(分量) "1_1","2_2","3_-2","4_0"
				{
					String column_maxtrix1 = column_value_matrix1.split("_")[0];
					//获得列号
					String value_matrix1 = column_value_matrix1.split("_")[1];
					//获得该列的值
					
					for(String column_value_matrix2 : column_value_array_matrix2)//对于右侧矩阵的line行的每一列(分量) "4_-1","1,-1","3_4","2_5"
					{
						String column_maxtrix2 = column_value_matrix2.split("_")[0];
						//获得列号
						String value_matrix2 = column_value_matrix2.split("_")[1];
						//获得该列的值
						
						if(column_maxtrix2.equals(column_maxtrix1))//这里也体现了为什么要标明列号，只有列号明确且相等，才证明是同一个位置的分量
						{
							result += Integer.valueOf(value_matrix1) * Integer.valueOf(value_matrix2);
							//result += 1 * (-1)
							//result += 2 * 5
							//result += -2 * 4
							//result += 0 * (-1)
						}
					}
				}
				outKey.set(row_matrix1);//输出的key值设置为左侧矩阵的行号
				outValue.set(row_matrix2 + "_" +result);//输出的value值设置为右侧转置矩阵的行号(实际矩阵的列号)_该位置的值
				context.write(outKey, outValue);
				//("1","3_1")
			}
			//("1","2_7")("1,"3_1")("1","2_4")("1","4_0")("1","5_9")
			//("2","1_9")...
			//....
		}
	}
	
	
	public static class Reducer2 extends Reducer<Text,Text,Text,Text>
	{
		private Text outKey = new Text();
		private Text outValue = new Text();
		
		/**
		 * 将map产生的key-value对进行组合，拼接成结果矩阵的物理形式
		 * ("1","2_7")("1,"3_1")("1","2_4")("1","4_0")("1","5_9")
		 * ("2","1_9")...
		 * ...
		 * 对于key值相同的元素("1","2_7")("1,"3_1")("1","2_4")("1","4_0")("1","5_9")
		 * 会将其组合
		 * key : "1"
		 * values : {"2_7","3_1","2_4","4_0","5_9"}
		 *
		 */
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			
			StringBuilder sb = new StringBuilder();
			for(Text text : values)
			{
				sb.append(text + ",");
			}
			// sb : "2_7,3_1,2_4,4_0,5_9,"
			String line = "";
			if(sb.toString().endsWith(","))
			{
				line = sb.substring(0,sb.length()-1);
			}
			//line :"2_7,3_1,2_4,4_0,5_9"
			outKey.set(key);
			outValue.set(line);
			context.write(outKey, outValue);
			// ("1","2_7,3_1,2_4,4_0,5_9")
		}
		
	}
	
	
	private static final String INPATH = "input/matrix.txt";
	private static final String OUTPATH = "hdfs://pc1:9000/output/step2_3";
	
	private static final String CACHE = "hdfs://pc1:9000/cache/matrix.txt";
	private static final String HDFS = "hdfs://pc1:9000";
	
	public void run() throws IOException, ClassNotFoundException, InterruptedException, URISyntaxException {
		 Configuration conf = new Configuration();
		    //String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		    //String[] otherArgs = {"hdfs://pc1:9000/input/chenjie.txt","hdfs://pc1:9000/output/out4"};
		    String[] otherArgs = {INPATH,OUTPATH};
		    //这里需要配置参数即输入和输出的HDFS的文件路径
		    if (otherArgs.length != 2) {
		      System.err.println("Usage: wordcount <in> <out>");
		      System.exit(2);
		    }
		    //conf.set("fs.defaultFS",HDFS);
		   // JobConf conf1 = new JobConf(WordCount.class);
		    Job job = new Job(conf, "step2");//Job(Configuration conf, String jobName) 设置job名称和
		    job.setJarByClass(Step2.class);
		    job.setMapperClass(Mapper2.class); //为job设置Mapper类 
		    //job.setCombinerClass(IntSumReducer.class); //为job设置Combiner类  
		    job.setReducerClass(Reducer2.class); //为job设置Reduce类 

		    job.addCacheArchive(new URI(CACHE + "#matrix2"));
		    
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
			new Step2().run();
		} catch (ClassNotFoundException | IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
