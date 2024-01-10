# PSI-B
1. Diego Amil González: ID 3

2. Comando para compilación en Linux usado:
sudo javac -cp .:jade.jar *.java agents/RandomAgent.java

Ejecución el Linux óptima:
java -cp .:jade.jar:agents jade.Boot -notmp -gui -agents "MainAgent:MainAgent;RandomAgent0:RandomAgent;RandomAgent1:RandomAgent;RandomAgent2:RandomAgent;RandomAgent0:RandomAgent;RandomAgent3:RandomAgent;RandomAgent4:RandomAgent;"

5. Descripcion funcionalidades deficientes:
-Solo borra un agente de cada vez. Si se borra otro, el anterior ocupa su posición.
-5 jugadores máximo.
-Reset Players reinicia solo el GUI, los payoff y demás datos de los jugadores no se reinician.
