import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/** Temporary CI experiment that models Maven copying a plugin while other threads fork. */
public class ProtocForkRace {
    public static void main(String[] args) throws Exception {
        Path source = Path.of(args[0]);
        Path plugin = Path.of(args[1]);
        Path protoc = Path.of(args[2]);
        Path protoDir = Path.of(args[3]);
        Path output = Path.of(args[4]);
        AtomicBoolean running = new AtomicBoolean(true);
        Thread[] forkers = new Thread[3];
        for (int i = 0; i < forkers.length; i++) {
            forkers[i] = new Thread(() -> {
                while (running.get()) {
                    try {
                        new ProcessBuilder("/bin/true").start().waitFor();
                        Thread.sleep(1);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            forkers[i].setDaemon(true);
            forkers[i].start();
        }
        try {
            for (int iteration = 1; iteration <= 200; iteration++) {
                File target = plugin.toFile();
                java.nio.file.Files.deleteIfExists(plugin);
                try (FileInputStream input = new FileInputStream(source.toFile());
                     FileOutputStream destination = new FileOutputStream(target)) {
                    byte[] buffer = new byte[65536];
                    int length;
                    while ((length = input.read(buffer)) != -1) {
                        destination.write(buffer, 0, length);
                    }
                }
                if (!target.setExecutable(true)) {
                    throw new IllegalStateException("Unable to mark generator executable");
                }
                int exit = new ProcessBuilder(protoc.toString(), "--proto_path=" + protoDir,
                        "--plugin=protoc-gen-grpc-java=" + plugin,
                        "--grpc-java_out=" + output, protoDir.resolve("contextTest.proto").toString())
                        .inheritIO().start().waitFor();
                if (exit != 0) {
                    throw new IllegalStateException("protoc failed at iteration " + iteration + ": " + exit);
                }
            }
            System.out.println("Completed 200 copy/fork/generate iterations");
        } finally {
            running.set(false);
            for (Thread forker : forkers) {
                forker.join();
            }
        }
    }
}
