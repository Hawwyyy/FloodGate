import com.android.ide.common.vectordrawable.Svg2Vector;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reproduce Android vectors from the downloaded Figma SVGs using Android's own importer. */
public class ConvertDashboardSvg {
    public static void main(String[] args) throws Exception {
        Path source = Path.of(args[0]);
        Path destination = Path.of(args[1]);
        try (var entries = Files.list(source)) {
            for (Path svg : entries.filter(p -> p.toString().endsWith(".svg")).sorted().toList()) {
                var output = new ByteArrayOutputStream();
                String warnings = Svg2Vector.parseSvgToXml(svg, output);
                if (warnings != null && !warnings.isBlank()) {
                    System.err.println(svg.getFileName() + ": " + warnings);
                }
                if (output.size() == 0) throw new IllegalStateException("Empty vector: " + svg);
                Path target = destination.resolve(svg.getFileName().toString().replace(".svg", ".xml"));
                Files.write(target, output.toByteArray());
                System.out.println(target);
            }
        }
    }
}
