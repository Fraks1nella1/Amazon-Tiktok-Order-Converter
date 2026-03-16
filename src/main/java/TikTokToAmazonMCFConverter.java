import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class TikTokToAmazonMCFConverter {

    private static final Map<String, String> STATE_MAP = new HashMap<>();
    private static final Map<String, String> SKU_MAP = new LinkedHashMap<>();

    private static final List<DateTimeFormatter> INPUT_DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.US),
            DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ss a", Locale.US),
            DateTimeFormatter.ofPattern("M/d/yyyy hh:mm:ss a", Locale.US),
            DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a", Locale.US)
    );

    private static final DateTimeFormatter OUTPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final List<String> OUTPUT_HEADERS = Arrays.asList(
            "MerchantFulfillmentOrderID",
            "DisplayableOrderID",
            "DisplayableOrderDate",
            "MerchantSKU",
            "Quantity",
            "MerchantFulfillmentOrderItemID",
            "GiftMessage",
            "DisplayableComment",
            "PerUnitDeclaredValue",
            "DisplayableOrderComment",
            "DeliverySLA",
            "AddressName",
            "AddressFieldOne",
            "AddressFieldTwo",
            "AddressFieldThree",
            "AddressCity",
            "AddressCountryCode",
            "AddressStateOrRegion",
            "AddressPostalCode",
            "AddressPhoneNumber",
            "NotificationEmail",
            "FulfillmentAction",
            "MarketplaceID",
            "CarrierPreferences"
    );

    private static String normalizeStateKey(String state) {
        if (state == null) {
            return "";
        }

        String clean = state.trim().toLowerCase(Locale.ROOT);

        if (clean.endsWith("州")) {
            clean = clean.substring(0, clean.length() - 1);
        }

        clean = clean.replace("\uFEFF", "").trim();
        return clean;
    }
    private static void loadStateMappings() {
        try (InputStream input = TikTokToAmazonMCFConverter.class
                .getClassLoader()
                .getResourceAsStream("states.properties")) {

            if (input == null) {
                throw new RuntimeException("states.properties 未找到");
            }

            Properties props = new Properties();
            props.load(new InputStreamReader(input, StandardCharsets.UTF_8));

            for (String key : props.stringPropertyNames()) {
                STATE_MAP.put(normalizeStateKey(key), props.getProperty(key).trim().toUpperCase(Locale.ROOT));
            }

            System.out.println("州名映射加载完成，共 " + STATE_MAP.size() + " 条");
        } catch (Exception e) {
            throw new RuntimeException("加载州名映射失败", e);
        }
    }
    public void run(String inputFile, Scanner scanner) throws Exception {
        //加载字典
        loadStateMappings();
        validateInputFile(inputFile);
        // 扫描SKU
        List<String> tiktokSkus = scanTikTokSkus(inputFile);

        if (tiktokSkus.isEmpty()) {
            System.out.println("未找到SKU信息，程序退出。");
            return;
        }

        System.out.println("在TikTok文件中找到以下SKU:");
        for (String sku : tiktokSkus) {
            System.out.println("TikTok SKU: " + sku);
            System.out.print("请输入对应的亚马逊后台SKU: ");
            String amazonSku = scanner.nextLine().trim();
            SKU_MAP.put(sku, amazonSku);
        }

        System.out.print("请输入输出文件路径（默认: amazon_mcf_output.csv）: ");
        String outputFile = scanner.nextLine().trim();

        if (outputFile.isEmpty()) {
            outputFile = "amazon_mcf_output.csv";
        }

        convertFile(inputFile, outputFile);

        System.out.println("转换完成！输出文件: " + outputFile);
    }

    private static void validateInputFile(String inputFile) throws IOException {
        Path path = Paths.get(inputFile);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在: " + inputFile);
        }
        if (!inputFile.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("当前程序仅支持 CSV 文件，请先将 Excel 另存为 CSV UTF-8。");
        }
    }

    private static List<String> scanTikTokSkus(String filePath) throws IOException {
        Set<String> skus = new LinkedHashSet<>();

        try (Reader reader = new InputStreamReader(
                BOMInputStream.builder()
                        .setPath(Paths.get(filePath))
                        .get(),
                StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            Map<String, String> headerMap = normalizeHeaderMap(parser.getHeaderMap().keySet());
            String skuColumn = findFirstExistingHeader(headerMap,
                    "Seller SKU", "SKU", "SellerSKU", "seller sku");

            if (skuColumn == null) {
                System.out.println("警告: 未找到SKU列。");
                return new ArrayList<>();
            }

            for (CSVRecord record : parser) {
                String sku = safeGet(record, skuColumn);
                if (!sku.isEmpty()) {
                    skus.add(sku);
                }
            }
        }

        return new ArrayList<>(skus);
    }

    private static void convertFile(String inputPath, String outputPath) throws IOException {
        try (Reader reader = new InputStreamReader(
                BOMInputStream.builder()
                        .setPath(Paths.get(inputPath))
                        .get(),
                StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader);
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath), StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            // 写 UTF-8 BOM，降低 Excel 打开乱码概率
            writer.write('\uFEFF');

            // 输出表头
            csvPrinter.printRecord(OUTPUT_HEADERS);

            Map<String, String> headerMap = normalizeHeaderMap(parser.getHeaderMap().keySet());

            String orderIdCol = requireHeader(headerMap, "Order ID");
            String createdTimeCol = requireHeader(headerMap, "Created Time");
            String recipientCol = requireHeader(headerMap, "Recipient");
            String address1Col = requireHeader(headerMap, "Address Line 1");
            String cityCol = requireHeader(headerMap, "City");
            String stateCol = requireHeader(headerMap, "State");
            String zipcodeCol = requireHeader(headerMap, "Zipcode");
            String phoneCol = requireHeader(headerMap, "Phone #");
            String quantityCol = requireHeader(headerMap, "Quantity");
            String sellerSkuCol = requireHeader(headerMap, "Seller SKU");

            String address2Col = findFirstExistingHeader(headerMap, "Address Line 2");
            String emailCol = findFirstExistingHeader(headerMap, "Buyer Email", "Email", "Notification Email");

            Set<String> usedOrderItemIds = new HashSet<>();
            int rowNum = 1;

            for (CSVRecord record : parser) {
                rowNum++;

                String orderId = safeGet(record, orderIdCol);
                String createdTime = safeGet(record, createdTimeCol);
                String recipient = safeGet(record, recipientCol);
                String address1 = safeGet(record, address1Col);
                String address2 = address2Col == null ? "" : safeGet(record, address2Col);
                String city = safeGet(record, cityCol);
                String state = safeGet(record, stateCol);
                String zipcode = safeGet(record, zipcodeCol);
                String phone = safeGet(record, phoneCol);
                String quantity = safeGet(record, quantityCol);
                String sellerSku = safeGet(record, sellerSkuCol);
                String notificationEmail = emailCol == null ? "" : safeGet(record, emailCol);

                String cleanOrderId = normalizeOrderId(orderId);
                if (cleanOrderId.isEmpty()) {
                    System.err.println("警告: 第 " + rowNum + " 行缺少 Order ID，已跳过。");
                    continue;
                }

                String merchantOrderId = cleanOrderId + "-T";
                String displayableOrderId = cleanOrderId + "-T";
                String merchantItemId = buildUniqueItemId(cleanOrderId, usedOrderItemIds);

                String merchantSku = SKU_MAP.getOrDefault(sellerSku, sellerSku);
                String displayableDate = parseAndFormatDate(createdTime, cleanOrderId);

                String stateAbbr = normalizeState(state);
                String postalCode = normalizePostalCode(zipcode);
                String cleanPhone = normalizePhone(phone);
                String cleanQuantity = quantity == null || quantity.trim().isEmpty() ? "1" : quantity.trim();

                List<String> outputRow = Arrays.asList(
                        merchantOrderId,
                        displayableOrderId,
                        displayableDate,
                        merchantSku,
                        cleanQuantity,
                        merchantItemId,
                        "",
                        "",
                        "",
                        "Thank you for your order!",
                        "Standard",
                        recipient,
                        address1,
                        address2,
                        "",
                        city,
                        "US",
                        stateAbbr,
                        postalCode,
                        cleanPhone,
                        notificationEmail,
                        "Ship",
                        "ATVPDKIKX0DER",
                        ""
                );

                csvPrinter.printRecord(outputRow);
            }

            csvPrinter.flush();
        }
    }

    private static Map<String, String> normalizeHeaderMap(Set<String> originalHeaders) {
        Map<String, String> normalized = new HashMap<>();
        for (String header : originalHeaders) {
            String clean = normalizeHeader(header);
            normalized.put(clean, header);
        }
        return normalized;
    }

    private static String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
    }

    private static String requireHeader(Map<String, String> headerMap, String... aliases) {
        String found = findFirstExistingHeader(headerMap, aliases);
        if (found == null) {
            throw new IllegalArgumentException("未找到必需列: " + Arrays.toString(aliases));
        }
        return found;
    }

    private static String findFirstExistingHeader(Map<String, String> headerMap, String... aliases) {
        for (String alias : aliases) {
            String match = headerMap.get(normalizeHeader(alias));
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static String safeGet(CSVRecord record, String columnName) {
        if (columnName == null || !record.isMapped(columnName)) {
            return "";
        }
        String value = record.get(columnName);
        return value == null ? "" : value.replace("\uFEFF", "").trim();
    }

    private static String normalizeOrderId(String orderId) {
        if (orderId == null) {
            return "";
        }
        return orderId.replace("\t", "").trim();
    }

    private static String buildUniqueItemId(String cleanOrderId, Set<String> usedIds) {
        String base = cleanOrderId + "-I";
        if (!usedIds.contains(base)) {
            usedIds.add(base);
            return base;
        }

        int counter = 2;
        while (true) {
            String candidate = base + counter;
            if (!usedIds.contains(candidate)) {
                usedIds.add(candidate);
                return candidate;
            }
            counter++;
        }
    }

    private static String parseAndFormatDate(String createdTime, String orderId) {
        String cleanTime = createdTime == null ? "" : createdTime.replace("\t", "").trim();
        if (cleanTime.isEmpty()) {
            System.err.println("警告: 订单 " + orderId + " 的 Created Time 为空。");
            return "";
        }

        for (DateTimeFormatter formatter : INPUT_DATE_FORMATTERS) {
            try {
                LocalDateTime dt = LocalDateTime.parse(cleanTime, formatter);
                return dt.format(OUTPUT_DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }

        System.err.println("日期格式转换错误，订单号: " + orderId + "，原值: [" + cleanTime + "]");
        return "";
    }

    private static String normalizeState(String state) {
        if (state == null) {
            return "";
        }

        String cleanKey = normalizeStateKey(state);
        if (cleanKey.isEmpty()) {
            return "";
        }

        // 已经是两位州缩写
        if (cleanKey.matches("^[a-z]{2}$")) {
            return cleanKey.toUpperCase(Locale.ROOT);
        }

        String mapped = STATE_MAP.get(cleanKey);
        if (mapped != null) {
            return mapped;
        }

        // 兼容去空格写法，如 "rhode island" -> "rhodeisland"
        mapped = STATE_MAP.get(cleanKey.replaceAll("\\s+", ""));
        if (mapped != null) {
            return mapped;
        }

        System.err.println("警告: 未识别州名: " + state);
        return state.trim();
    }

    private static String normalizePostalCode(String zipcode) {
        if (zipcode == null) {
            return "";
        }

        String digits = zipcode.replaceAll("[^0-9]", "");
        if (digits.length() == 4) {
            return "0" + digits;
        }
        if (digits.length() >= 5) {
            return digits.substring(0, 5);
        }
        return digits;
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }

        String digits = phone.replaceAll("[^0-9]", "");

        // 11位且以1开头，去掉美国国家码
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }

        if (!digits.isEmpty() && digits.length() < 10) {
            System.err.println("警告: 电话号码位数异常: " + phone + " -> " + digits);
        }

        return digits;
    }
    public List<String> scanSkusForUi(String inputFile) throws Exception {
        STATE_MAP.clear();
        loadStateMappings();
        validateInputFile(inputFile);
        return scanTikTokSkus(inputFile);
    }
    public void convert(String inputFile, String outputFile, Map<String, String> skuMap) throws Exception {
        STATE_MAP.clear();
        SKU_MAP.clear();

        loadStateMappings();
        validateInputFile(inputFile);

        SKU_MAP.putAll(skuMap);

        convertFile(inputFile, outputFile);
    }
}