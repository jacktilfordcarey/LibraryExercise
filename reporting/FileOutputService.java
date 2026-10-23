package reporting;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Iterator;

public class FileOutputService implements OutputService {
	private final Path path;
	
	public FileOutputService(String path) throws IOException {
		if (path == null || path.trim().length() == 0) {
			throw new IllegalArgumentException("Output path is required");
		}
		this.path = Paths.get(path);
		Files.newBufferedWriter(this.path, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).close();
	}
	
	public synchronized void outputExtent(String title, Collection subscribers) throws IOException {
		if (title == null || subscribers == null) {
			throw new NullPointerException("Report title and collection are required");
		}
		for (Object contributor : subscribers) {
			if (!(contributor instanceof OutputServiceContributor)) {
				throw new IllegalArgumentException("Report item does not provide formatted output");
			}
		}
		// Append each report so multiple output calls share one complete report file.
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
			writer.write(title);
			writer.newLine();
			writer.write("-----------");
			writer.newLine();
			for (Iterator iter = subscribers.iterator(); iter.hasNext();) {
				OutputServiceContributor contributor = (OutputServiceContributor)iter.next();
				writer.write(contributor.getFormattedString());
				writer.newLine();
			}
			writer.newLine();
		}
	}
	
	public String getName() {
		return "File output service";
	}
}
