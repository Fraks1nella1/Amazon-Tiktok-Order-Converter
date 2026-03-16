import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

    private final JTextField inputFileField = new JTextField();
    private final JTextField outputDirField = new JTextField();
    private final JTextArea logArea = new JTextArea();

    private final JLabel statusLabel = new JLabel("准备就绪");
    private final JButton startBtn = new JButton("开始转换");
    private final JButton chooseInputBtn = new JButton("选择 CSV");
    private final JButton chooseOutputBtn = new JButton("选择输出文件夹");

    private final TikTokToAmazonMCFConverter converter = new TikTokToAmazonMCFConverter();

    public MainFrame() {
        setTitle("TikTok → Amazon MCF 转换工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 640));
        setSize(980, 700);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(18, 18, 18, 18));
        setContentPane(root);

        root.add(createHeaderPanel(), BorderLayout.NORTH);
        root.add(createCenterPanel(), BorderLayout.CENTER);
        root.add(createBottomPanel(), BorderLayout.SOUTH);

        bindActions();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 238)),
                new EmptyBorder(20, 22, 20, 22)
        ));
        panel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("TikTok → Amazon MCF 转换工具");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(31, 78, 121));

        JLabel subTitleLabel = new JLabel("适用于出海电商订单转换：选择 CSV、配置 SKU 映射、生成 Amazon MCF 文件");
        subTitleLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        subTitleLabel.setForeground(new Color(100, 116, 139));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(subTitleLabel);

        panel.add(textPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        panel.add(createFileCard(), BorderLayout.NORTH);
        panel.add(createLogCard(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFileCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 238)),
                new EmptyBorder(18, 18, 18, 18)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 8, 10, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel sectionTitle = new JLabel("文件设置");
        sectionTitle.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
        sectionTitle.setForeground(new Color(31, 41, 55));

        inputFileField.setPreferredSize(new Dimension(0, 38));
        outputDirField.setPreferredSize(new Dimension(0, 38));

        chooseInputBtn.setPreferredSize(new Dimension(150, 38));
        chooseOutputBtn.setPreferredSize(new Dimension(150, 38));
        startBtn.setPreferredSize(new Dimension(160, 42));
        startBtn.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        startBtn.setBackground(new Color(31, 78, 121));
        startBtn.setForeground(Color.WHITE);
        startBtn.setFocusPainted(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        card.add(sectionTitle, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        card.add(new JLabel("输入文件"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        card.add(inputFileField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        card.add(chooseInputBtn, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weightx = 0;
        card.add(new JLabel("输出文件夹"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        card.add(outputDirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        card.add(chooseOutputBtn, gbc);

        gbc.gridy = 3;
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        card.add(startBtn, gbc);

        return card;
    }

    private JPanel createLogCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 238)),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel logTitle = new JLabel("运行日志");
        logTitle.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
        logTitle.setForeground(new Color(31, 41, 55));

        JLabel hintLabel = new JLabel("显示扫描、映射、转换结果与错误信息");
        hintLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        hintLabel.setForeground(new Color(107, 114, 128));

        titlePanel.add(logTitle, BorderLayout.WEST);
        titlePanel.add(hintLabel, BorderLayout.EAST);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setBackground(new Color(250, 252, 255));
        logArea.setForeground(new Color(31, 41, 55));
        logArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        card.add(titlePanel, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 238)),
                new EmptyBorder(10, 14, 10, 14)
        ));

        statusLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(75, 85, 99));

        JLabel footerLabel = new JLabel("Cross-border Utility · TikTok Order to Amazon MCF");
        footerLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        footerLabel.setForeground(new Color(148, 163, 184));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(footerLabel, BorderLayout.EAST);

        return panel;
    }

    private void bindActions() {
        chooseInputBtn.addActionListener(e -> chooseInputFile());
        chooseOutputBtn.addActionListener(e -> chooseOutputDirectory());
        startBtn.addActionListener(e -> startConvert());
    }

    private void chooseInputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择 TikTok 订单 CSV 文件");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            inputFileField.setText(chooser.getSelectedFile().getAbsolutePath());
            appendLog("已选择输入文件: " + chooser.getSelectedFile().getAbsolutePath());
            setStatus("已选择输入文件");
        }
    }

    private void chooseOutputDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择输出文件夹");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            outputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            appendLog("已选择输出文件夹: " + chooser.getSelectedFile().getAbsolutePath());
            setStatus("已选择输出文件夹");
        }
    }

    private void startConvert() {
        String inputFile = inputFileField.getText().trim();
        String outputDir = outputDirField.getText().trim();

        if (inputFile.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择输入 CSV 文件", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (outputDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择输出文件夹", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!Files.exists(Paths.get(inputFile))) {
            JOptionPane.showMessageDialog(this, "输入文件不存在", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isDirectory(Paths.get(outputDir))) {
            JOptionPane.showMessageDialog(this, "输出路径不是有效文件夹", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        startBtn.setEnabled(false);
        setStatus("正在处理中...");
        appendLog("开始处理任务...");

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            private String finalOutputFile;

            @Override
            protected Void doInBackground() throws Exception {
                publish("开始扫描 SKU...");
                List<String> skus = converter.scanSkusForUi(inputFile);

                if (skus.isEmpty()) {
                    throw new RuntimeException("未扫描到 SKU");
                }

                publish("共扫描到 " + skus.size() + " 个 SKU，准备填写映射...");

                Map<String, String> skuMap = showSkuMappingDialog(skus);
                if (skuMap == null) {
                    throw new RuntimeException("用户取消了 SKU 映射");
                }

                String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                finalOutputFile = Paths.get(outputDir, "amazon_mcf_Tiktok" + time + ".csv").toString();

                publish("开始转换文件...");
                converter.convert(inputFile, finalOutputFile, skuMap);
                publish("转换完成: " + finalOutputFile);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    appendLog(msg);
                }
            }

            @Override
            protected void done() {
                startBtn.setEnabled(true);
                try {
                    get();
                    setStatus("转换完成");
                    JOptionPane.showMessageDialog(
                            MainFrame.this,
                            "转换完成！\n输出文件：\n" + finalOutputFile,
                            "成功",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    appendLog("转换失败: " + msg);
                    setStatus("转换失败");
                    JOptionPane.showMessageDialog(
                            MainFrame.this,
                            "转换失败：\n" + msg,
                            "错误",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    private Map<String, String> showSkuMappingDialog(List<String> skus) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel tip = new JLabel("请填写 TikTok SKU 对应的 Amazon SKU，留空则默认使用原 SKU");
        tip.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        tip.setForeground(new Color(75, 85, 99));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);

        Map<String, JTextField> fields = new LinkedHashMap<>();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        for (String sku : skus) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            formPanel.add(new JLabel("TikTok SKU: " + sku), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            JTextField tf = new JTextField(20);
            tf.setPreferredSize(new Dimension(0, 34));
            fields.put(sku, tf);
            formPanel.add(tf, gbc);

            row++;
        }

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setPreferredSize(new Dimension(620, Math.min(420, 90 + skus.size() * 48)));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));

        panel.add(tip, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "SKU 映射配置",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        Map<String, String> skuMap = new LinkedHashMap<>();
        for (Map.Entry<String, JTextField> entry : fields.entrySet()) {
            String amazonSku = entry.getValue().getText().trim();
            if (amazonSku.isEmpty()) {
                amazonSku = entry.getKey();
            }
            skuMap.put(entry.getKey(), amazonSku);
        }

        return skuMap;
    }

    private void appendLog(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        logArea.append("[" + time + "] " + msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }
}