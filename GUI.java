import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class GUI extends JFrame implements ActionListener {
    JLabel leftPanelRoundsLabel;
    JLabel leftPanelGamesLabel;
    JLabel leftPanelNGamesLabel;
    JLabel leftPanelExtraInformation;
    JList<String> list;
    private MainAgent mainAgent;
    private JPanel rightPanel;
    private JTable payoffTable;
    private JTextArea rightPanelLoggingTextArea;
    private LoggingOutputStream loggingOutputStream;
    int rounds=10;

    public GUI() {
        initUI();
    }

    public GUI (MainAgent agent) {
        mainAgent = agent;
        initUI();
        loggingOutputStream = new LoggingOutputStream (rightPanelLoggingTextArea);
    }

    public void log (String s) {
        Runnable appendLine = () -> {
            rightPanelLoggingTextArea.append('[' + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + s);
            rightPanelLoggingTextArea.setCaretPosition(rightPanelLoggingTextArea.getDocument().getLength());
        };
        SwingUtilities.invokeLater(appendLine);
    }

    public OutputStream getLoggingOutputStream() {
        return loggingOutputStream;
    }

    public void logLine (String s) {
        log(s + "\n");
    }

    public void setPlayersUI (String[] players) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
 //       int i=0;
        for (String s : players) {
            listModel.addElement(s);
            // String[] name = s.split("@");
            // payoffTable.setValueAt(name[0], 0, i +1);
            // payoffTable.setValueAt(0, 12, i+1);
            // i++;
        }
        list.setModel(listModel);
    }
    public void deletePlayersUI (){
        if(list.getSelectedValue() != null){
  //          list.remove(list.getSelectedIndex());
            mainAgent.eliminarPlayer(list.getSelectedValue());
        }
    }

    public void resetTableInformation(){
        for (int i = 1; i<=11; i++){
            for (int j=1; j<=5; j++){
                payoffTable.setValueAt(0, i, j);
 //               mainAgent.setPayoff0();
            }
        }
    }

    public void setRoundsUI(int rondas){
        leftPanelRoundsLabel.setText("Rounds: " + rondas);
    }

    public void setNGamesUI(int nGames){
        leftPanelGamesLabel.setText("Number of Games: " + nGames);
    }
    public void setNPlayersUI(int nplayers){
        leftPanelNGamesLabel.setText("Number of Players: " + nplayers);
    }

    public void salir(){
        System.exit(-1);
    }

    public void setPlayerAction (int eleccion, int round, int columna){
        payoffTable.setValueAt(eleccion, round+1, columna);
    }
    
    public void setPlayerResult (int resultado, int columna){
        payoffTable.setValueAt(resultado, 11, columna);
    }

    public void initUI() {
        setTitle("Hawk & Dove GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(600, 400));
        setPreferredSize(new Dimension(1280, 720));
        setJMenuBar(createMainMenuBar());
        setContentPane(createMainContentPane());
        pack();
        setVisible(true);
    }

    private Container createMainContentPane() {
        JPanel pane = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridy = 0;
        gc.weightx = 0.5;
        gc.weighty = 0.5;

        //LEFT PANEL
        gc.gridx = 0;
        gc.weightx = 1;
        pane.add(createLeftPanel(), gc);

        //CENTRAL PANEL
        gc.gridx = 1;
        gc.weightx = 8;
        pane.add(createCentralPanel(), gc);

        //RIGHT PANEL
        gc.gridx = 2;
        gc.weightx = 8;
        pane.add(createRightPanel(), gc);
        return pane;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        leftPanelRoundsLabel = new JLabel("Rounds: " + rounds);
        leftPanelGamesLabel = new JLabel("Number of Games: 0");
        leftPanelNGamesLabel = new JLabel("Number of Players: 5");
        JButton leftPanelNewButton = new JButton("New");
        leftPanelNewButton.addActionListener(actionEvent -> mainAgent.newGame());
        //nGames++;
        JButton leftPanelStopButton = new JButton("Stop");
        leftPanelStopButton.addActionListener(actionEvent -> mainAgent.pausar());
        JButton leftPanelContinueButton = new JButton("Continue");
        leftPanelContinueButton.addActionListener(actionEvent -> mainAgent.continuar());

        leftPanelExtraInformation = new JLabel("Parameters:");
        JLabel mensajeRondas = new JLabel("Number of Rounds");
        JButton updateNumberOfRounds = new JButton("OK");
        JTextField campoTextoRondas = new JTextField();
        updateNumberOfRounds.addActionListener(actionEvent -> mainAgent.updateRounds(Integer.parseInt(campoTextoRondas.getText())));
        


        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;
        gc.weightx = 0.7;
        gc.weighty = 0.5;

        gc.gridy = 0;
        leftPanel.add(leftPanelExtraInformation, gc);
        gc.gridy = 1;
        leftPanel.add(leftPanelRoundsLabel, gc);
        gc.gridy = 2;
        leftPanel.add(leftPanelGamesLabel, gc);
        gc.gridy = 3;
        leftPanel.add(leftPanelNGamesLabel, gc);
        gc.gridy = 4;
        leftPanel.add(leftPanelNewButton, gc);
        gc.gridy = 5;
        leftPanel.add(leftPanelStopButton, gc);
        gc.gridy = 6;
        leftPanel.add(leftPanelContinueButton, gc);
//        gc.weighty = 6;
        gc.gridy = 7;
        leftPanel.add(mensajeRondas, gc);
        gc.gridy = 8;
        leftPanel.add(campoTextoRondas, gc);
        gc.weighty = 10;
        gc.gridy = 9;
        leftPanel.add(updateNumberOfRounds, gc);

    //    gc.weighty = 10;

        return leftPanel;
    }

    private JPanel createCentralPanel() {
        JPanel centralPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;

        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;

        gc.gridy = 0;
        gc.weighty = 1;
        centralPanel.add(createCentralTopSubpanel(), gc);
        gc.gridy = 1;
        gc.weighty = 4;
        centralPanel.add(createCentralBottomSubpanel(), gc);

        return centralPanel;
    }

    private JPanel createCentralTopSubpanel() {
        JPanel centralTopSubpanel = new JPanel(new GridBagLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("Empty");
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(list);

        JLabel info1 = new JLabel("<--- Select the player to remove");
        /*JButton updatePlayersButton = new JButton("Update players");
        updatePlayersButton.addActionListener(actionEvent -> mainAgent.updatePlayers());*/
        JButton deletePlayerButton = new JButton("Remove");
        deletePlayerButton.addActionListener(actionEvent -> deletePlayersUI());

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.weighty = 0.5;
        gc.anchor = GridBagConstraints.CENTER;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 666;
        gc.fill = GridBagConstraints.BOTH;
        centralTopSubpanel.add(listScrollPane, gc);
        gc.gridx = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        centralTopSubpanel.add(info1, gc);
        gc.gridy = 1;
        centralTopSubpanel.add(deletePlayerButton, gc);

        return centralTopSubpanel;
    }

    private JPanel createCentralBottomSubpanel() {
        JPanel centralBottomSubpanel = new JPanel(new GridBagLayout());

        Object[] nullPointerWorkAround = {"*", "*", "*", "*", "*", "*"};

        Object[][] data = {
            {"", "ID1", "ID2", "ID3", "ID4", "ID5"},
            {"Round 1", "*", "*", "*", "*", "*"},
            {"Round 2", "*", "*", "*", "*", "*"},
            {"Round 3", "*", "*", "*", "*", "*"},
            {"Round 4", "*", "*", "*", "*", "*"},
            {"Round 5", "*", "*", "*", "*", "*"},
            {"Round 6", "*", "*", "*", "*", "*"},
            {"Round 7", "*", "*", "*", "*", "*"},
            {"Round 8", "*", "*", "*", "*", "*"},
            {"Round 9", "*", "*", "*", "*", "*"},
            {"Round 10", "*", "*", "*", "*", "*"},
            {"Total Payoff", "*", "*", "*", "*", "*"},
    };
        JLabel payoffLabel = new JLabel("Player Results");
        payoffTable = new JTable(data, nullPointerWorkAround);
        payoffTable.setTableHeader(null);
        payoffTable.setEnabled(false);
        
        JScrollPane player1ScrollPane = new JScrollPane(payoffTable);

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weighty = 0.5;
        centralBottomSubpanel.add(payoffLabel, gc);
        gc.gridy = 1;
        gc.gridx = 0;
        gc.weighty = 2;
        centralBottomSubpanel.add(player1ScrollPane, gc);

        return centralBottomSubpanel;
    }

    private JPanel createRightPanel() {
        rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.weighty = 1d;
        c.weightx = 1d;

        rightPanelLoggingTextArea = new JTextArea("");
        rightPanelLoggingTextArea.setEditable(false);
        JScrollPane jScrollPane = new JScrollPane(rightPanelLoggingTextArea);
        rightPanel.add(jScrollPane, c);
        return rightPanel;
    }

    private JMenuBar createMainMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        JMenuItem exitFileMenu = new JMenuItem("Exit");
        exitFileMenu.setToolTipText("Exit application");
        exitFileMenu.addActionListener(actionEvent -> salir());

        /*JMenuItem newGameFileMenu = new JMenuItem("New Game");
        newGameFileMenu.setToolTipText("Start a new game");
        newGameFileMenu.addActionListener(this);

        menuFile.add(newGameFileMenu);*/
        menuFile.add(exitFileMenu);
        menuBar.add(menuFile);

       JMenu menuEdit = new JMenu("Edit");
        JMenuItem resetPlayerEditMenu = new JMenuItem("Reset Players");
        resetPlayerEditMenu.setToolTipText("Reset all player");
        resetPlayerEditMenu.addActionListener(actionEvent -> resetTableInformation());

  /*      JMenuItem parametersEditMenu = new JMenuItem("Remove Players");
        parametersEditMenu.setToolTipText("Modify the parameters of the game");
        parametersEditMenu.addActionListener(actionEvent -> logLine("Parameters: " + JOptionPane.showInputDialog(new Frame("Configure parameters"), "Enter parameters N,R")));
*/ 
        menuEdit.add(resetPlayerEditMenu);
 //       menuEdit.add(parametersEditMenu);
        menuBar.add(menuEdit);

        /*JMenu menuRun = new JMenu("Run");

        JMenuItem newRunMenu = new JMenuItem("New");
        newRunMenu.setToolTipText("Starts a new series of games");
        newRunMenu.addActionListener(this);

        JMenuItem stopRunMenu = new JMenuItem("Stop");
        stopRunMenu.setToolTipText("Stops the execution of the current round");
        stopRunMenu.addActionListener(this);

        JMenuItem continueRunMenu = new JMenuItem("Continue");
        continueRunMenu.setToolTipText("Resume the execution");
        continueRunMenu.addActionListener(this);*/

        /*JMenuItem roundNumberRunMenu = new JMenuItem("Number of rounds");
        roundNumberRunMenu.setToolTipText("Change the number of rounds");
        roundNumberRunMenu.addActionListener(actionEvent -> logLine(JOptionPane.showInputDialog(new Frame("Configure rounds"), "How many rounds?") + " rounds"));
        menuRun.add(newRunMenu);
        menuRun.add(stopRunMenu);
        menuRun.add(continueRunMenu);
        menuRun.add(roundNumberRunMenu);
        menuBar.add(menuRun);*/

        JMenu menuWindow = new JMenu("Window");

        JCheckBoxMenuItem toggleVerboseWindowMenu = new JCheckBoxMenuItem("Verbose", true);
        toggleVerboseWindowMenu.addActionListener(actionEvent -> rightPanel.setVisible(toggleVerboseWindowMenu.getState()));

        menuWindow.add(toggleVerboseWindowMenu);
        menuBar.add(menuWindow);

        JMenu menuHelp = new JMenu("Help");
        JMenuItem aboutMenu = new JMenuItem("About");
        aboutMenu.addActionListener(action -> JOptionPane.showMessageDialog(new Frame("Info about this program"), "Hawk and Dove Game, by Diego Amil González"));
        menuHelp.add(aboutMenu);
        menuBar.add(menuHelp);

        return menuBar;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton button = (JButton) e.getSource();
 //           logLine("Button " + button.getText());
        } else if (e.getSource() instanceof JMenuItem) {
            JMenuItem menuItem = (JMenuItem) e.getSource();
   //         logLine("Menu " + menuItem.getText());
        }
    }

    public class LoggingOutputStream extends OutputStream {
        private JTextArea textArea;

        public LoggingOutputStream(JTextArea jTextArea) {
            textArea = jTextArea;
        }

        @Override
        public void write(int i) throws IOException {
            textArea.append(String.valueOf((char) i));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }
}
