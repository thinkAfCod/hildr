package io.optimism.rpc;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.Key;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.web3j.utils.Numeric;

public class SplitCapFileCopyTest {

    private static ObjectMapper mapper = new ObjectMapper();

    private static final Key key = Keys.hmacShaKeyFor(
            Numeric.hexStringToByteArray("bf549f5188556ce0951048ef467ec93067bc4ea21acebe46ef675cd4e8e015ff"));

    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            handleFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static final String urlOpGeth = "http://127.0.0.1:9551";
    static final String urlOpBesu = "http://127.0.0.1:8551";

    static void handleFile() throws IOException, InterruptedException {
        String outputDirPath = "/Users/xiaqingchuan/WorkSpace/github/rollup/request_rollop_new";
        File jsonsDir = new File(outputDirPath);
        if (!jsonsDir.exists() || jsonsDir.isFile()) {
            return;
        }

        List<File> sortedFiles = loadFiles(jsonsDir, ".json", "_request.debug.json");
        List<File> sortedZippedFiles = loadFiles(new File(outputDirPath, "zipped"), ".zip", "_request.debug.json.zip");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.MINUTES)
                .build();

        // op-geth record
        var startIndex = 323;
        var endIndex = 0;
        final var id = 1414257;
        //         v24 besu record
        //        var startIndex = 24;
        //        var id = 13171914;
        final var endId = 0;

        var hasFound = new AtomicBoolean(false);
        int fileSize = sortedZippedFiles.size();
        AtomicReference<BigInteger> lastBlockRef = new AtomicReference<>(BigInteger.ZERO);


        for (int i = startIndex; i < fileSize; i++) {
            var jsonZippedFile = sortedZippedFiles.get(i);
            var unzippedDir = jsonZippedFile.getParentFile().getParentFile();
            File jsonFile = new File(unzippedDir, jsonZippedFile.getName().replace(".zip", ""));
            boolean unzippedProcess = !jsonFile.exists();
            if (unzippedProcess) {
                unzip(jsonZippedFile, unzippedDir);
            }

            sendRequest(client, jsonFile, id, hasFound, i, endIndex, endId);
//            verifyRequest(jsonFile, lastBlockRef);
            if (unzippedProcess) {
                jsonFile.delete();
            }
        }
    }

    private static void sendRequest(
        final OkHttpClient client,
        final File jsonFile,
        final int id,
        final AtomicBoolean hasFound,
        final int curIndex,
        final int endIndex,
        final int endId) {
        readFileInLine(jsonFile, json -> {
            Object jsonRpcId = json.get("id");
            if (jsonRpcId == null) {
                throw new IllegalArgumentException("wrong json");
            }
            if (id != 0 && !hasFound.get()) {
                if (jsonRpcId.equals(id)) {
                    hasFound.set(true);
                } else {
                    return;
                }
            }
            if (endIndex != 0) {
                if (curIndex == endIndex && endId != 0 && jsonRpcId.equals(endId)) {
                    throw new IllegalArgumentException("touched endId: " + endId);
                }
            } else if (endId != 0 && jsonRpcId.equals(endId)) {
                throw new IllegalArgumentException("touched endId: " + endId);
            }

            RequestBody requestBody = null;
            byte[] jsonBytes = null;
            String debugViewJson = null;
            try {
                jsonBytes = mapper.writeValueAsBytes(json);
                debugViewJson = new String(jsonBytes);
                requestBody = RequestBody.create(jsonBytes, MediaType.get("application/json"));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            var besuTask = sendRequest(client, urlOpBesu, requestBody);
            var besuMap = besuTask.get();
            checkResult(jsonBytes, besuMap);
//                var gethTask = sendRequest(client, urlOpGeth, requestBody);
//                var gethMap = gethTask.get();
//                checkResult(jsonBytes, gethMap);
        });
    }

    private static void verifyRequest(final File jsonFile, final AtomicReference<BigInteger> lastBlockRef) {
        readFileInLine(jsonFile, json -> {
            String method = (String) json.get("method");
            if (method.startsWith("engine_newPayload")) {
                try {
                    var blockNumber = (String) ((List<Map<String, Object>>) json.get("params"))
                        .get(0)
                        .get("blockNumber");
                    var curBlockNum = Numeric.toBigInt(blockNumber);
                    var last = lastBlockRef.get();
                    if (!last.equals(BigInteger.ZERO) && curBlockNum.subtract(last).compareTo(BigInteger.ONE) != 0) {
                        throw new RuntimeException(String.format(
                            "found a wrong json in file: %s, value: %s",
                            jsonFile.getName(), mapper.writeValueAsString(json)));
                    }
                    lastBlockRef.set(curBlockNum);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static void unzip(File jsonZippedFile, File destDir) {
        // create output directory if it doesn't exist
        if(!destDir.exists()) destDir.mkdirs();
        FileInputStream fis = null;
        ZipInputStream zis = null;
        //buffer for read and write data to file
        byte[] buffer = new byte[10240];
      try {
          fis = new FileInputStream(jsonZippedFile);
          zis = new ZipInputStream(fis);
          ZipEntry ze = zis.getNextEntry();
          while(ze != null){
              String fileName = ze.getName();
              File newFile = new File(destDir + File.separator + fileName);
              System.out.println("Unzipping to "+newFile.getAbsolutePath());
              //create directories for sub directories in zip
              new File(newFile.getParent()).mkdirs();
              FileOutputStream fos = new FileOutputStream(newFile);
              int len;
              while ((len = zis.read(buffer)) > 0) {
                  fos.write(buffer, 0, len);
              }
              fos.close();
              //close this ZipEntry
              zis.closeEntry();
              ze = zis.getNextEntry();
          }
          //close last ZipEntry
          zis.closeEntry();
          zis.close();
          fis.close();
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException("unzip file error: " + jsonZippedFile.getName());
      } finally {
          try {
              if (zis != null) {
                  zis.close();
              }
              if (fis != null) {
                  fis.close();
              }
          } catch (IOException e) {
              e.printStackTrace();
          }

      }
    }

    private static List<File> loadFiles(final File jsonsDir, final String postfix, final String removeStr) {
        File[] files = jsonsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(postfix);
            }
        });
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        return Arrays.stream(files)
                .sorted(new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        var f1NoStr = f1.getName().replace(removeStr, "");
                        var f2NoStr = f2.getName().replace(removeStr, "");
                        return Integer.compare(Integer.parseInt(f1NoStr), Integer.parseInt(f2NoStr));
                    }
                })
                .toList();
    }

    private static void readFileInLine(File file, Consumer<Map<String, ?>> handler) {
        System.out.println("will read file:" + file.getName());
        try (BufferedReader br = Files.newBufferedReader(file.toPath(), UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                var sendData = mapper.readValue(line, new TypeReference<Map<String, ?>>() {});
                handler.accept(sendData);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String jwt(final Key key) {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        Date expirationDate = Date.from(now.plusSeconds(60));
        return Jwts.builder()
                .setIssuedAt(nowDate)
                .setExpiration(expirationDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private static StructuredTaskScope.Subtask<Map<String, Object>> sendRequest(
            OkHttpClient client, String url, RequestBody requestBody) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            StructuredTaskScope.Subtask<Map<String, Object>> fork = scope.fork(() -> {
                Request req = new Request.Builder()
                        .addHeader("authorization", String.format("Bearer %1$s", jwt(key)))
                        .post(requestBody)
                        .url(url)
                        .build();
                Response resp = client.newCall(req).execute();
                var resMap = mapper.readValue(resp.body().byteStream(), new TypeReference<Map<String, Object>>() {});
                resp.close();
                return resMap;
            });
            scope.join();
            scope.throwIfFailed();
            return fork;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkResult(byte[] jsonStr, Map<String, Object> result) {
        if (result.get("result") == null || result.get("error") != null) {
            System.out.println("throw error when send json: " + new String(jsonStr, UTF_8));
            System.out.println(result);
            throw new RuntimeException("throw error when send json");
        }
        Map<String, Object> jsonRpcResult = (Map<String, Object>) result.get("result");
        var status = (String) jsonRpcResult.get("status");
        if (status == null) {
            Map<String, Object> payloadStatus = (Map<String, Object>) jsonRpcResult.get("payloadStatus");
            if (payloadStatus != null) {
                status = (String) ((Map<String, Object>) jsonRpcResult.get("payloadStatus")).get("status");
            }
        }
        if (status == null) {
            if (jsonRpcResult.get("executionPayload") == null) {
                System.out.println("throw error when send json: " + new String(jsonStr, UTF_8));
                System.out.println(result);
                throw new RuntimeException("throw error when send json");
            } else {
                return;
            }
        }
        if (!"VALID".equals(status)) {
            System.out.println("throw error when send json: " + new String(jsonStr, UTF_8));
            System.out.println(result);
            throw new RuntimeException("throw error when send json");
        }
    }
}
