package udf;

import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.io.Text;

public class ConcatString extends UDF {

	public Text evaluate(Text a,Text b){
		return new Text(a.toString() +"****" + b.toString());
	}
}
