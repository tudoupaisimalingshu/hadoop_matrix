package udf;

import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.io.Text;

public class CheckSalaryGrade extends UDF {

	public Text evaluate(Text salary){
		//定义返回的级别
		Text grade = null;
		
		double sal = Double.parseDouble(salary.toString());
		//判断薪水的范围
		if(sal < 1000){
			grade = new Text("Grade A");
		}else if(sal>=1000 && sal < 3000){
			grade = new Text("Grade B");
		}else{
			grade = new Text("Grade C");
		}
		return grade;
	}
}







