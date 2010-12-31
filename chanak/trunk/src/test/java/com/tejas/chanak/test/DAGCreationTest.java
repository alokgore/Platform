package com.tejas.chanak.test;

import org.junit.Test;

import com.tejas.chanak.testutils.FailureDAG;
import com.tejas.chanak.testutils.PentagonDAG;
import com.tejas.chanak.testutils.StraightLineDAG;

public class DAGCreationTest extends DAGManagerTestCaseBase
{
    @Test
	public void test() throws Exception
	{
		new PentagonDAG().start();
		new FailureDAG().start();
		new StraightLineDAG().start();
	}
}
