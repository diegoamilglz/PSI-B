
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import agents.RandomAgent;

import java.io.PrintStream;
import java.util.ArrayList;

public class MainAgent extends Agent {

    private GUI gui;
    private AID[] playerAgents;
    private AID[] playerRemoved = new AID[50];
    //ArrayList<PlayerInformation> players = new ArrayList<>();
    private GameParametersStruct parameters = new GameParametersStruct();
    //int R,N;

    private int contDeleted=0;
    private int nGames=0;//,puntuacion=0,puntosTotales=0;
    private boolean gameStopped;
    Object lock = new Object();

    @Override
    protected void setup() {
        gui = new GUI(this);
   //     System.setOut(new PrintStream(gui.getLoggingOutputStream()));
        updatePlayers();
        gui.logLine("Agent " + getAID().getName() + " is ready.");
    }

    public int updatePlayers() {
        gui.logLine("Updating player list");
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
//                gui.logLine("Found " + result.length + " players");
                parameters.N=result.length;
            }
            playerAgents = new AID[result.length];
            for (int i = 0; i < result.length; ++i) {
                playerAgents[i] = result[i].getName();
            }
        } catch (FIPAException fe) {
            gui.logLine(fe.getMessage());
        }
        //Provisional
        String[] playerNames = new String[playerAgents.length];
        for (int i = 0; i < playerAgents.length; i++) {
            playerNames[i] = playerAgents[i].getName();
        }
        gui.setNPlayersUI(parameters.N);
        gui.setPlayersUI(playerNames);
        return 0;
    }

    public void eliminarPlayer(String player){
        boolean eliminado = false;
        gui.logLine("Removing player " + player);
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            for(int i = 0; i < result.length; i++) {
                //We search for the player we ant to remove
                if(result[i].getName().equals(player)){
                    eliminado = true;
                    playerAgents = new AID[result.length-1];
                    int x = 0;
                    //If the player is finded, we rebuild the matrix without the
                    //removed player
                    for(int j = 0; j < result.length; j++){
                        if(i != j){
                            playerAgents[x] = result[j].getName();
                            x++;
                        }
                    }
                    String[] playerNames = new String[playerAgents.length];

                    for (int k = 0; k < playerAgents.length; k++) {
                        playerNames[k] = playerAgents[k].getName();
                    }
                    gui.setPlayersUI(playerNames);
                    break;
                }
            }
            //This condition is just for print that the player we want to remove
            //hasnt't been found. If we print this at the else of the if at line 76,
            //this mesage would be printed at every iteration until the player is found
            if (!eliminado){
                gui.logLine("Player: " + player + " not found");
            }
        } catch (FIPAException e) {
            gui.logLine(e.getMessage());
        }
    }
    public void updateRounds(int rondas){
//        gui.logLine("Rondas elegidas: " + rondas);
        // R=rondas;
        parameters.R=rondas;
        gui.setRoundsUI(rondas);
    }

    public void updateNGames(int nGames){
//        gui.logLine("Games jugados: " + nGames);
        // numero de games
        gui.setNGamesUI(nGames);
    }

    // public void updateNPlayers(){
    //     gui.logLine("Rondas eleidas: " + rondas);
    //     parameters.N=players;
    //     gui.setNPlayersUI(nplayers);
    // }


    public void pausar() {
        gameStopped=true;
    }

    public void continuar(){
        if(gameStopped){
            synchronized(lock){
                gameStopped = false;
                lock.notifyAll();
            }
        }
    }

    public void resetPlayers () {

        /*for (PlayerInformation player : players) {

            player.payoff = 0;
            //gui.updatePlayer(player.id);
        }*/
    }

    // public void setPayoff0(){
    //     for (PlayerInformation player : players){
    //           player.payoff=0;
    //     }
    // }

    public int newGame() {
        addBehaviour(new GameManager());
        return 0;
    }

    
    /**
     * In this behavior this agent manages the course of a match during all the
     * rounds.
     */
    private class GameManager extends SimpleBehaviour {
        int bestP = 0;
        @Override
        
        public void action() {
            //Assign the IDs
            ArrayList<PlayerInformation> players = new ArrayList<>();
            int lastId = 0;
            gameStopped = false;
            for (AID a : playerAgents) {
                players.add(new PlayerInformation(a, lastId++, 0, 0));
            }

            //Initialize (inform ID)
            for (PlayerInformation player : players) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Id#" + player.id + "#" + parameters.N + "," + parameters.R);
                msg.addReceiver(player.aid);
                send(msg);
            }
            resetPlayers();
            //Organize the matches
            for (int i = 0; i < players.size(); i++) {
                for (int j = i + 1; j < players.size(); j++) {
                    synchronized (lock){
                        if(gameStopped){
                            try{lock.wait();
                            }
                            catch(InterruptedException e){
                                
                            }
                        }
                    }
                    playGame(players.get(i), players.get(j));
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){

                    }
                }
            }
            for (int i = 1; i < players.size (); i++) {

                if (players.get (i).payoff > players.get (bestP).payoff) {

                    bestP = i;
                }
            }
            System.out.println ("\n\n\n**** WINNER ****");
            System.out.println ("*          " + players.get (bestP).aid.getName ().split ("@") [0] + ": "+ players.get (bestP).payoff+ "          *");
            System.out.println ("****************");
            bestP=0;
        }

        private void playGame(PlayerInformation player1, PlayerInformation player2) {
            //Assuming player1.id < player2.id
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(player1.aid);
            msg.addReceiver(player2.aid);
            msg.setContent("NewGame#" + player1.id + "#" + player2.id);
            send(msg);
            int r=0;
            String action1, action2;
            for (int i=0; i < parameters.R; i++){
                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("Action");
                msg.addReceiver(player1.aid);
                send(msg);

//                gui.logLine("Main Waiting for movement");
                ACLMessage move1 = blockingReceive();
 //               gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
                action1 = move1.getContent().split("#")[1];

                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("Action");
                msg.addReceiver(player2.aid);
                send(msg);

 //               gui.logLine("Main Waiting for movement");
                ACLMessage move2 = blockingReceive();
 //               gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
                action2 = move2.getContent().split("#")[1];
   //             System.out.println(action1);
  //              System.out.println(action2);

                if("H".equals(action1)){

                    if("H".equals(action2)) {
                        player1.result=-1;
                        player2.result=-1;
   //                                     System.out.println(player1.result);

                    }else if ("D".equals(action2)){
                        player1.result=10;
                        player2.result=0;
 //                                       System.out.println(player1.result);

                    }
                }else if ("D".equals(action1)){
                    if("H".equals(action2)){
                        player1.result=0;
                        player2.result=10;
                    }else if ("D".equals(action2)){
                        player1.result=5;
                        player2.result=5;
                    }
                }
                msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(player1.aid);
                msg.addReceiver(player2.aid);
                msg.setContent("Results#"+player1.id+ ","+player2.id+"#"+action1 +","+action2+"#"+ player1.result+","+ player2.result); 
                send(msg);
                player1.payoff += player1.result;
                player2.payoff += player2.result;
                
                gui.setPlayerAction(player1.result, r, (player1.id + 1));
                gui.setPlayerAction(player2.result, r, (player2.id + 1));

                r++;
                if (r==10)r=0;
        }
                gui.setPlayerResult(player1.payoff, (player1.id + 1));
                gui.setPlayerResult(player2.payoff, (player2.id + 1));
            //fin bucle
            msg.setContent("GameOver#"+player1.id+","+player2.id+"#"+player1.payoff+","+player2.payoff);
            nGames++;
            updateNGames(nGames);
            send(msg);
        }

        @Override
        public boolean done() {
            return true;
        }
    }

    public class PlayerInformation {

        AID aid;
        int id;
        int result;
        int payoff;

        public PlayerInformation(AID a, int i, int puntuacion, int resultado) {
            aid = a;
            id = i;
            result=puntuacion;
            payoff= resultado;
        }

        @Override
        public boolean equals(Object o) {
            return aid.equals(o);
        }
    }

    public class GameParametersStruct {
        int N;
        int R;

        public GameParametersStruct() {
            N = 5;
            R = 10; //por defecto
        }
    }
}
