package com.tejas.utils.console.system;

import com.tejas.utils.console.ListOptionsServlet;

public class SystemConsoleOptionsServlet extends ListOptionsServlet
{
	public SystemConsoleOptionsServlet()
	{
		super(
			new ConsoleEntry("Threads"),
			new ConsoleEntry("Summary", ThreadSummaryServlet.class, true),
			new ConsoleEntry("Details", ThreadDetailServlet.class),
			new ConsoleEntry(),
			
			new ConsoleEntry("Stats"),
			new ConsoleEntry("ps", PsServlet.class),
			new ConsoleEntry("top", TopServlet.class),
			new ConsoleEntry("df", DfServlet.class),
			new ConsoleEntry("iostat", IoStatServlet.class),
			new ConsoleEntry(),
			
			new ConsoleEntry("Logs"),
			new ConsoleEntry("log", ListLogsServlet.class)
			);
	}
}
