package partizionamento;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        
        System.out.println("Il programma accetta solo indirizzi di classe A, B o C");
        System.out.println("Inserisci l'indirizzo, es. 192.168.0.1: ");
        String ip = scan.next();
        System.out.println("Inserisci il numero di sottoreti in cui vuoi partizionare la rete: ");
        int subnets = scan.nextInt();
        int[] hosts = new int[subnets];
        for(int i=0; i<subnets; i++){
            System.out.println("Inserisci il numero di host della " + (i+1) + "^ sottorete");
            hosts[i] = scan.nextInt();
        }
        
        Partizionamento p1;
        System.out.println("Conosci il prefix length, la maschera di rete, o nessuno dei due? [1-2-3 per scegliere]");
        switch(scan.nextInt()){
            case 1 ->{
                System.out.println("Inserisci il prefix length: ");
                int prefix = scan.nextInt();
                p1 = new Partizionamento(ip, prefix, subnets, hosts);
            }
            case 2 ->{
                System.out.println("Inserisci la maschera di rete: es. 255.255.192.0");
                String netmask = scan.next();
                p1 = new Partizionamento(ip, netmask, subnets, hosts);
            }
            default ->{
                System.out.println("La maschera di rete verrà assegnata di default");
                p1 = new Partizionamento(ip, -1, subnets, hosts); //viene dato -1 come prefixLength 
            }
        }
        
        
        System.out.println("\n" + p1.getResults());
        
    }
    
}
