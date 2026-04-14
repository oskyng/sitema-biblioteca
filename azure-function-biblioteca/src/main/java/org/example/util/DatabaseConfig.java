package org.example.util;

import com.microsoft.azure.functions.ExecutionContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DatabaseConfig {
    private static final String WALLET_DIR_NAME = "Wallet_LaboratoryManagement";
    private static final String[] WALLET_FILES = {
            "cwallet.sso", "ewallet.p12", "ewallet.pem", "keystore.jks",
            "ojdbc.properties", "sqlnet.ora", "tnsnames.ora", "truststore.jks"
    };

    public static Connection getConnection(ExecutionContext context) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new Exception("Missing environment variables: DB_URL, DB_USER, or DB_PASSWORD.");
        }

        File tempDir = new File(System.getProperty("java.io.tmpdir"), WALLET_DIR_NAME);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        for (String fileName : WALLET_FILES) {
            File file = new File(tempDir, fileName);
            if (!file.exists()) {
                String resourcePath = WALLET_DIR_NAME + "/" + fileName;
                try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream(resourcePath)) {
                    if (is != null) {
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                }
            }
        }

        String tnsAdmin = tempDir.getAbsolutePath();
        Properties props = new Properties();
        props.setProperty("user", dbUser);
        props.setProperty("password", dbPassword);
        props.setProperty("oracle.net.tns_admin", tnsAdmin);
        props.setProperty("oracle.net.wallet_location", tnsAdmin);

        Class.forName("oracle.jdbc.OracleDriver");
        return DriverManager.getConnection(dbUrl, props);
    }
}
