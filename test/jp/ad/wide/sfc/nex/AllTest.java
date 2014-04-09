package jp.ad.wide.sfc.nex;


import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;


public class AllTest {
    public static void main(String[] args){
	Result result = null;
	boolean alltest = true;
	if (alltest){
	    result = JUnitCore.runClasses(
					  SimpleDocumentStreamImplTest.class,
					  LoopTest.class,
					  PreprocessorTest.class,
					  PostprocessorTest.class,
					  EXIDocumentStreamTest.class,
					  XMLfilterReaderTest.class,
					  XMLTester.class,
					  DummyTest.class
					  );
	} else {
	    System.out.println("Subtest.");
	    result = JUnitCore.runClasses(
					  SimpleDocumentStreamImplTest.class,
					  DummyTest.class
					  );
	}
	System.out.println("\n----\n");
	if (result.getFailureCount() > 0){
	    for (Failure failure : result.getFailures()){
		System.out.println(failure.toString());
		Throwable e = failure.getException();
		if (e != null){
		    e.printStackTrace();
		}
	    }
	    System.out.println(result.getFailureCount()+" test(s) FAILED out of "+result.getRunCount());
	    System.exit(1);
	} else {
	    System.out.println("\n---\nall "+result.getRunCount()+" test(s) passed successfully.");
	}
	if (result.getIgnoreCount() > 0){
	    System.out.println(result.getIgnoreCount()+" test(s) are maked as ignored.");
	}
    }
}


