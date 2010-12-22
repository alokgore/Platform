package com.tejas.utils.console.system;

import com.tejas.utils.console.ListOptionsServlet;

public class SystemConsoleOptionsServlet extends ListOptionsServlet
{
	public SystemConsoleOptionsServlet()
	{
		super(
			new ConsoleEntry("Threads"),
			new ConsoleEntry("Summary", ThreadSummaryServlet.class, true),
			new ConsoleEntry("Details", ThreadDetailServlet.class, true),
			new ConsoleEntry(),
			
			new ConsoleEntry("Stats"),
			new ConsoleEntry("ps", PsServlet.class, true),
			new ConsoleEntry("top", TopServlet.class, true),
			new ConsoleEntry("df", DfServlet.class, true),
//			new ConsoleEntry("iostat", IoStatServlet.class, true),
			new ConsoleEntry(),
			
			new ConsoleEntry("Logs"),
			new ConsoleEntry("log", ListLogsServlet.class, true)
			);
	}
}
