package agents;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.io.Serializable;
import java.util.Random;
import java.util.Vector;

public class mi_RL extends Agent {

    private State state;
    private AID mainAgent;
    private int myId, opponentId;
    private int N, R;
    private int result, opponentResult;//resultado final;
    private ACLMessage msg;

    private String EstadoRL;
    private char eleccion;
    private LearningTools learningTools;

    protected void setup() {
        state = State.s0NoConfig;
    learningTools = new LearningTools();
        //Register in the yellow pages as a player
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        sd.setName("Game");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new Play());
        System.out.println("RL_Agent " + getAID().getName() + " is ready.");

    }

    protected void takeDown() {
        //Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println("RL_Agent " + getAID().getName() + " terminating.");
    }

    private enum State {
        s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
    }

    private class Play extends CyclicBehaviour {
        Random random = new Random(1000);
        @Override
        public void action() {
 //           System.out.println(getAID().getName() + ":" + state.name());
            msg = blockingReceive();
            if (msg != null) {
 //               System.out.println(getAID().getName() + " received " + msg.getContent() + " from " + msg.getSender().getName()); //DELETEME
                //-------- Agent logic
                switch (state) {
                    case s0NoConfig:
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> go to state 1
                        //Else ERROR
                        if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
                            boolean parametersUpdated = false;
                            try { 
                                parametersUpdated = validateSetupMessage(msg);
                                state = State.s1AwaitingGame;
                            } catch (NumberFormatException e) {
                                System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                            }
                            break;
                            //if (parametersUpdated) state =State.s1AwaitingGame;

                        } 
                    
                        
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        
                        break;
                    case s1AwaitingGame:
                        //If INFORM NEWGAME#_,_ PROCESS NEWGAME --> go to state 2
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> stay at s1
                        //Else ERROR
                        //TODO I probably should check if the new game message comes from the main agent who sent the parameters
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            if (msg.getContent().startsWith("Id#")) { //Game settings updated
                                try {
                                    validateSetupMessage(msg);
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                            } else if (msg.getContent().startsWith("NewGame#")) {
                                boolean gameStarted = false;
                                try {
                                    gameStarted = validateNewGame(msg.getContent());
                                    state = State.s2Round;
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                                break;
                                //if (gameStarted) state = State.s2Round;
                            }
                        
                        }
                        break;
                    case s2Round:
                        //If REQUEST POSITION --> INFORM POSITION --> go to state 3
                        //If INFORM CHANGED stay at state 2
                        //If INFORM ENDGAME go to state 1
                        //Else error
                        if (msg.getPerformative() == ACLMessage.REQUEST /*&& msg.getContent().startsWith("Position")*/) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(mainAgent);
                            if(EstadoRL != null){
                                learningTools.vGetNewActionQLearning(EstadoRL, 2, result);
                                int selectedActionIndex = learningTools.iNewAction;
                                //eleccion = letters[selectedActionIndex];
                                if (selectedActionIndex==1) {
                                    msg.setContent("Action#H");
                                    eleccion='H';
                                }
                                else {
                                    msg.setContent("Action#D");
                                    eleccion='D';
                                }
                                send(msg);
                            }
                            else{
                                System.err.println("Como va esto");
                                int opcion= random.nextInt(2);
                                if (opcion==0) {
                                    msg.setContent("Action#H");
                                    eleccion='H';
                                }
                                else {
                                    msg.setContent("Action#D");
                                    eleccion='D';
                                }
    //                           }System.out.println(getAID().getName() + " sent " + msg.getContent());
                                send(msg);
                            }
                            state = State.s3AwaitingResult;
                        //} else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Changed#")) {
                            // Process changed message, in this case nothing
                        } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("GameOver#")) {
                            state = State.s1AwaitingGame;
                            //guardarPayoff(msg.getContent());
                        
                        } 
                        break;
                    case s3AwaitingResult:
                        if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
                            //Process results
                            System.out.println(msg.getContent());
                            guardarResultado(msg.getContent());
                            state = State.s2Round;
                        }
                }
            }
        }

        /**
         * Validates and extracts the parameters from the setup message
         *
         * @param msg ACLMessage to process
         * @return true on success, false on failure
         */
        private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
            int tN, tR, tMyId;
            String msgContent = msg.getContent();

            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 3) return false;
            if (!contentSplit[0].equals("Id")) return false;
            tMyId = Integer.parseInt(contentSplit[1]);

            String[] parametersSplit = contentSplit[2].split(",");
            if (parametersSplit.length != 2) return false;
            tN = Integer.parseInt(parametersSplit[0]);
            tR = Integer.parseInt(parametersSplit[1]);

            //At this point everything should be fine, updating class variables
            mainAgent = msg.getSender();
            N = tN;
            R = tR;
            myId = tMyId;
            return true;
        }

        /**
         * Processes the contents of the New Game message
         * @param msgContent Content of the message
         * @return true if the message is valid
         */
        public boolean validateNewGame(String msgContent) {
            int msgId0, msgId1;
            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 3) return false;
            if (!contentSplit[0].equals("NewGame")) 
                return false;
            msgId0 = Integer.parseInt(contentSplit[1]);
            msgId1 = Integer.parseInt(contentSplit[2]);
            if (myId == msgId0) {
                opponentId = msgId1;
                return true;
            } else if (myId == msgId1) {
                opponentId = msgId0;
                return true;
            }
            return false;
        }
        
        // public void guardarPayoff(String msgContent) {
        //     int msgId0, msgId1;
        //     String[] contentSplit = msgContent.split("#");
        //     String[] idSplit = contentSplit[1].split(",");
        //     String[] payoffSplit = contentSplit[2].split(",");
        //     msgId0 = Integer.parseInt(idSplit[0]);
        //     msgId1 = Integer.parseInt(idSplit[1]);
        //     if (myId == msgId0) {
        //         payoff=Integer.parseInt(payoffSplit[0]);
        //         opponentPayoff=Integer.parseInt(payoffSplit[1]);
        //     } else if (myId == msgId1) {
        //         payoff=Integer.parseInt(payoffSplit[1]);
        //         opponentPayoff=Integer.parseInt(payoffSplit[0]);
        //     }
        // }
        public void guardarResultado(String msgContent) {
            int msgId0, msgId1;
            String[] contentSplit = msgContent.split("#");
            String[] idSplit = contentSplit[1].split(",");
            String[] decSplit = contentSplit[2].split(",");
            String[] resSplit = contentSplit[3].split(",");
            msgId0 = Integer.parseInt(idSplit[0]);
            msgId1 = Integer.parseInt(idSplit[1]);
            if (myId == msgId0) {
                result=Integer.parseInt(resSplit[0]);
                opponentResult=Integer.parseInt(resSplit[1]);
            } else if (myId == msgId1) {
                result=Integer.parseInt(resSplit[1]);
                opponentResult=Integer.parseInt(resSplit[0]);
            }
            System.err.println("Resultado que es suyo: " + result);
            char dec1 = decSplit[0].charAt(0);
            char dec2 = decSplit[1].charAt(0);

                                System.err.println("Hola?");
            if (eleccion == dec1){
                EstadoRL = String.valueOf(dec1) + String.valueOf(dec2);
            } 
            if (eleccion == dec2){
                EstadoRL = String.valueOf(dec2) + String.valueOf(dec1);
            }

        }
  // from class LearningTools

    }
}
           /**
         * This is a basic class with some learning tools: statistical learning,
         * learning automata (LA) and Q-Learning (QL)
         *
         * @author Juan C. Burguillo Rial
         * @version 2.0
         */
        class LearningTools {
            final double dDecFactorLR = 0.70; // Value that will decrement the learning rate in each generation
            final double dEpsilon = 0.75; // Used to avoid selecting always the best action
            final double dMINLearnRate = 0.05; // We keep learning, after convergence, during 5% of times
            final double dGamma = 0.95;
            double dLearnRate = 0.65; 

            boolean bAllActions = false; // At the beginning we did not try all actions
            int iNewAction; // This is the new action to be played
            int iNumActions = 2; // For H or D for instance
            int iLastAction; // The last action that has been played by this player
            int[] iNumTimesAction = new int[iNumActions]; // Number of times an action has been played
            double[] dPayoffAction = new double[iNumActions]; // Accumulated payoff obtained by the different actions
            StateAction oLastStateAction;
            StateAction oPresentStateAction;
            Vector<StateAction> oVStateActions = new Vector<>();; // A vector containing strings with the possible States and
                                                                    // Actions available at each one

                                                                    /**
             * This method is used to implement Q-Learning:
             * 1. I start with the last action a, the previous state s and find the actual
             * state s'
             * 2. Select the new action with Qmax{a'}
             * 3. Adjust: Q(s,a) = Q(s,a) + dLearnRateLR [R + dGamma . Qmax{a'}(s',a') -
             * Q(s,a)]
             * 4. Select the new action by a epsilon-greedy methodology
             *
             * @param sState    contains the present state
             * @param iNActions contains the number of actions that can be applied in this
             *                  state
             * @param dReward   is the reward obtained after performing the last action.
             */

            public void vGetNewActionQLearning(String sState, int iNActions, int dReward) {
                boolean bFound;
                int iBest = -1, iNumBest = 1;
                double dR, dQmax;
                StateAction oStateAction;

                bFound = false; // Searching if we already have the state
                // if(oVStateActions != null){
                for (int i = 0; i < oVStateActions.size(); i++) {
                oStateAction = (StateAction) oVStateActions.elementAt(i);
                if (oStateAction.sState.equals(sState)) {
                    oPresentStateAction = oStateAction;
                    bFound = true;
                    break;
                }
                // }
                }
                // If we didn't find it, then we add it
                if (!bFound) {
                    oPresentStateAction = new StateAction(sState, iNActions);
                    oVStateActions.add(oPresentStateAction);
                }
            
                dQmax = 0;
                for (int i = 0; i < iNActions; i++) { // Determining the action to get Qmax{a'}
                    if (oPresentStateAction.dValAction[i] > dQmax) {
                    iBest = i;
                    iNumBest = 1; // Reseting the number of best actions
                    dQmax = oPresentStateAction.dValAction[i];
                    } else if ((oPresentStateAction.dValAction[i] == dQmax) && (dQmax > 0)) { // If there is another one equal we must
                                                                                            // select one of them randomly
                    iNumBest++;
                    if (Math.random() < 1.0 / (double) iNumBest) { // Choose randomly with reducing probabilities
                        iBest = i;
                        dQmax = oPresentStateAction.dValAction[i];
                    }
                    }
                }
                // Adjusting Q(s,a) using the formula
                if (oLastStateAction != null)
                    oLastStateAction.dValAction[iLastAction] += dLearnRate
                        * (dReward + dGamma * dQmax - oLastStateAction.dValAction[iLastAction]);
            
                if ((iBest > -1) && (Math.random() > dEpsilon)) // Using the e-greedy policy to select the best action or any of the
                                                                // rest
                    iNewAction = iBest;
                else
                    do {
                    iNewAction = (int) (Math.random() * (double) iNumActions);
                    } while (iNewAction == iBest);
            
                oLastStateAction = oPresentStateAction; // Updating values for the next time
                dLearnRate *= dDecFactorLR; // Reducing the learning rate
                if (dLearnRate < dMINLearnRate)
                    dLearnRate = dMINLearnRate;
                }
        
        }
        /**
         * This is the basic class to store Q values (or probabilities) and actions for
         * a certain state
         *
         * @author Juan C. Burguillo Rial
         * @version 2.0
         */

        class StateAction implements Serializable {
        String sState;
        double[] dValAction;

        StateAction(String sAuxState, int iNActions) {
            sState = sAuxState;
            dValAction = new double[iNActions];
        }

        StateAction(String sAuxState, int iNActions, boolean bLA) {
            this(sAuxState, iNActions);
            if (bLA)
            for (int i = 0; i < iNActions; i++) // This constructor is used for LA and sets up initial probabilities
                dValAction[i] = 1.0 / iNActions;
        }

        public String sGetState() {
            return sState;
        }

        public double dGetQAction(int i) {
            return dValAction[i];
        }
        }