import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileHasher {

    // Creates a new instance of MessageDigest for SHA-256
    private MessageDigest createMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    // Calculate the hash of a single file
    public String calculateHash(File file) throws IOException, NoSuchAlgorithmException {
        if (!file.exists() || !file.canRead()) {
            throw new IOException("File does not exist or is not readable: " + file.getPath());
        }

        MessageDigest md = createMessageDigest();
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = md.digest();

        // Convert byte array to a hexadecimal string
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    // Calculate hashes for all files in a directory and its subdirectories
    public Map<String, List<String>> calculateHashes(File file) throws IOException {
        Map<String, List<String>> fileHashes = new HashMap<>();

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    Map<String, List<String>> childHashes = calculateHashes(child);
                    // Merge childHashes into fileHashes
                    childHashes.forEach((key, value) ->
                            fileHashes.computeIfAbsent(key, k -> new ArrayList<>()).addAll(value)
                    );
                }
            }
        } else if (file.isFile()) {
            String hash = null;
            try {
                hash = calculateHash(file);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            fileHashes.computeIfAbsent(hash, k -> new ArrayList<>()).add(file.getCanonicalPath());
        }

        return fileHashes;
    }

    // Check if a file is a duplicate based on the calculated hashes
    public boolean isFileDuplicate(Map<String, List<String>> fileHashes, File file) throws IOException {
        String hash = null;
        try {
            hash = calculateHash(file);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        if (fileHashes.containsKey(hash)) {
            for (String path : fileHashes.get(hash)) {
                if (!new File(path).getCanonicalPath().equals(file.getCanonicalPath())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Get the names of duplicate files for a given file
    public List<String> giveDuplicateFileNames(Map<String, List<String>> fileHashes, File file) throws IOException {
        String hash = null;
        try {
            hash = calculateHash(file);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        List<String> duplicateFiles = fileHashes.getOrDefault(hash, new ArrayList<>());

        List<String> filteredDuplicates = new ArrayList<>();
        for (String path : duplicateFiles) {
            if (!new File(path).getCanonicalPath().equals(file.getCanonicalPath())) {
                filteredDuplicates.add(path);
            }
        }
        return filteredDuplicates;
    }

    // Example test
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        FileHasher hasher = new FileHasher();

        // Directory to calculate hashes
        File directory = new File("test_directory");
        Map<String, List<String>> fileHashes = hasher.calculateHashes(directory);

        // File to check for duplicates
        File fileToCheck = new File("test_directory/file_to_check.txt");

        boolean isDuplicate = hasher.isFileDuplicate(fileHashes, fileToCheck);
        if (isDuplicate) {
            List<String> duplicates = hasher.giveDuplicateFileNames(fileHashes, fileToCheck);
            System.out.println("The file has duplicates:");
            duplicates.forEach(System.out::println);
        } else {
            System.out.println("The file does not have duplicates.");
        }
    }
}
