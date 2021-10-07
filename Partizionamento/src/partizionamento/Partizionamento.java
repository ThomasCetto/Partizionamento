package partizionamento;

import java.util.Arrays;

public class Partizionamento {
    
    private final String ip;
    private int[] bytes = new int[4];
    private final char ipClass;
    private final boolean isPrivate;
    private String netmask="";
    private int[] netmaskBytes = new int[4];
    private int prefixLength;
    private int netBytes;
    private int hostBytes;
    
    //sottoreti
    private int subnets;
    private int[] hosts;
    private String[] hostsInBinary;
    private int[] bitsNeededForSubnets;
    private int[] bitsNeededForHosts;
    private String[] subnetMask;
    private String[] subnetBits;
    private String[][] addresses;
    private String[][] subnetBitsPerByte;
    private String[][] hostBitsPerByte;
    
    
    public Partizionamento(String ip, int prefix, int subnets, int[] hosts){
        this.ip = ip;
        this.prefixLength = prefix;
        bytes = turnIntoBytes(ip);
        ipClass = calculateAddressClass();
        isPrivate = isPrivateAddress();
        if(prefix != -1)
            calculateNetmask();
        else{
            calculateDefaultMask();
            prefixLength = calculatePrefixLength();
        }
        
        //sottoreti
        initializeSubnetsAttributes(subnets, hosts);
        
    }
    
    public Partizionamento(String ip, String netmask, int subnets, int[] hosts){
        this.ip = ip;
        this.netmask = netmask;
        bytes = turnIntoBytes(ip);
        netmaskBytes = turnIntoBytes(netmask);
        ipClass = calculateAddressClass();
        isPrivate = isPrivateAddress();
        prefixLength = calculatePrefixLength();
        
        //sottoreti
        initializeSubnetsAttributes(subnets, hosts);
        
    }
    
    private void initializeSubnetsAttributes(int subnets, int[] hosts){
        this.subnets = subnets;
        this.hosts = hosts;
        calculateNetAndHostBytes();
        this.bitsNeededForSubnets = calculateBitsForSubnet();
        this.bitsNeededForHosts = calculateBitsForHosts();
        this.subnetBits = new String[subnets];
        this.addresses = new String[subnets][7]; //7 indirizzi diversi per ogni sottorete
        this.subnetBitsPerByte = new String[subnets][4]; //4 per ogni byte
        this.hostBitsPerByte = new String[subnets][4];
        this.subnetBits = calculateSubnetBits();
        this.subnetMask = calculateSubnetMask();
        
        for(int i=0; i<addresses.length; i++){
            for(int j=0; j<addresses[i].length; j++){
                addresses[i][j] = "";
            }
        }
        
        calculateSubnetBitsPerSubnet();
        hostsInBinary = calculateHostsInBinary();
        
        this.addresses = calculateAddresses();
        
    }
    
    private String[][] calculateAddresses(){
        String[][] address = addresses;
        
        //per ogni sottorete
        for(int i=0; i<subnets; i++){
            address[i][0] = calculateBroadcastAddress(i);
            address[i][1] = calculateNetAddress(i);
            address[i][2] = calculateFirstHostAddress(i);
            address[i][3] = calculateLastHostAddress(i);
            address[i][6] = calculateDefaultGatewayAddress(i); //calcola prima questo cosi' si può sottrarre 1 da questo ind per trovare l'ultimo libero
            address[i][4] = calculateFirstFreeAddress(i);
            address[i][5] = calculateLastFreeAddress(i);
            
        }
        return address;
    }
    
    private String calculateBroadcastAddress(int subnetIndex){
        String address = addNetBytes();
        
        //aggiunge per tutti i byte degli host
        for(int i=0; i<hostBytes; i++){
            String add="";
            
            int bitsLimit = 8 - subnetBitsPerByte[subnetIndex][netBytes+i].length();
            for(int j=0; j<bitsLimit; j++){
                add += "1";
            }
            address += Integer.parseInt(subnetBitsPerByte[subnetIndex][netBytes+i] + add, 2);
            address += ".";
            
        }
        
        //toglie il punto finale
        return address.substring(0, address.length()-1);
    }
    
    private String calculateNetAddress(int subnetIndex){
        String address = addNetBytes();
        
        for(int i=0; i<hostBytes; i++){
            String add="";
            
            int bitsLimit = 8 - subnetBitsPerByte[subnetIndex][netBytes+i].length();
            for(int j=0; j<bitsLimit; j++){
                add += "0";
            }
            address += Integer.parseInt(subnetBitsPerByte[subnetIndex][netBytes+i] + add, 2);
            address += ".";
        }
        
        //toglie il punto finale
        return address.substring(0, address.length()-1);
    }
    
    private String calculateFirstHostAddress(int subnetIndex){
        return incrementAddress(addresses[subnetIndex][1]); //indirizzo dopo quello di rete
    }
    
    private String calculateLastHostAddress(int subnetIndex){
        String address = addNetBytes();
        
        int bitsDone=0;
        int bytesDone=0;
        String[] bytes = new String[4];
        for(int i=0; i<4; i++){
            bytes[i] = "";
        }
        
        for(int i=hostsInBinary[subnetIndex].length()-1; i>=0; i--){
            bytes[3-bytesDone] += hostsInBinary[subnetIndex].charAt(i);
            bitsDone++;
            if(bitsDone % 8 == 0)
                bytesDone++;
        }
        
        for(int i=0; i<4; i++){ //inverte ogni byte
            bytes[i] = invertString(bytes[i]);     
        }
        
        for(int i=netBytes; i<4; i++){
            String temp="";
            if(subnetBitsPerByte[subnetIndex][i].length() != 0)
                temp += subnetBitsPerByte[subnetIndex][i];
            if(bytes[i].length() != 0)
                temp += bytes[i];
            
            address += Integer.parseInt(temp, 2);
            address += ".";
        }
        
        //toglie il punto finale
        return address.substring(0, address.length()-1);
    }
    
    private String calculateFirstFreeAddress(int subnetIndex){
        return incrementAddress(addresses[subnetIndex][3]); //prende l'indirizzo dopo quello dell'ultimo host
    }
    
    private String calculateLastFreeAddress(int subnetIndex){
        return decrementAddress(addresses[subnetIndex][6]);
    }
    
    public String calculateDefaultGatewayAddress(int subnetIndex){
        return decrementAddress(addresses[subnetIndex][0]);
    }
    
    private String[] calculateHostsInBinary(){
        String[] arr = new String[subnets];
        for(int i=0; i<subnets; i++){
            arr[i] = Integer.toBinaryString(hosts[i]);
        }
        return arr;
    }
    
    private String[] calculateSubnetMask(){
        String[] mask = new String[subnets];
        
        //per ogni sottorete
        for(int j=0; j<subnets; j++){
            int count = netBytes*8 + bitsNeededForSubnets[j]; //i bit da mettere a 1
            mask[j]="";
            int bytesDone=0;
        
            while(count >= 8){ //aggiunge un 255 per ogni multiplo di 8
                mask[j]+="255.";
                count-=8;
                bytesDone++;
            }
        
             
            while(bytesDone < 4){//per i byte restanti li calcola aggiungendo singolarmente gli 1 e convertendoli
                String s="";
                for(int i=0; i<8; i++){ //un byte
                    s += (count>0)? "1" : "0";
                    count--;
                }
                bytesDone++;
                
                mask[j] += Integer.parseInt(s, 2);
                
                if(bytesDone!=4)
                    mask[j] += ".";
            }   
        }
        return mask;
    }
    
    private void calculateSubnetBitsPerSubnet(){
        for(int i=0; i<subnets; i++){ //per ogni sottorete
            int index=0;
            for(int j=0; j<subnetBitsPerByte[i].length; j++){ //per ogni byte
                if(j<netBytes){
                    subnetBitsPerByte[i][j] = "";
                }
                else{
                    if(subnetBits[i].substring(index).length() >= 8){ //se i bit di sottorete sono più di 8
                        subnetBitsPerByte[i][j] = subnetBits[i].substring(index, index + 8);
                        index+=8;
                    }
                    else{ //aggiunge solo i bit avanzati
                        subnetBitsPerByte[i][j] = subnetBits[i].substring(index, subnetBits[i].length());
                        index += subnetBits[i].length() - index;
                    }
                }
            }
        }
    }
    
    private String[] calculateSubnetBits(){
        String[] array = new String[subnets];
        array[0] = "";
        for(int k=0; k<bitsNeededForSubnets[0]; k++)
            array[0]+="0";
        
        for(int i=1; i<subnets; i++){
            array[i] = "";
            
            //contiene il codice binario dei bit della sottorete
            String temp = Integer.toBinaryString(Integer.parseInt(array[i-1], 2) + 1);
            
            for(int j=0; j<bitsNeededForSubnets[i] - bitsNeededForSubnets[i-1]; j++){
                temp+="0";
            }
            
            for(int j=0; j<bitsNeededForSubnets[i] - temp.length(); j++){ //aggiunge gli 0 necessari prima del numero effettivo
                array[i]+="0";
            }
            
            array[i]+=temp;
        }
        
        return array;
    }
    
    /*In base agli host di ogni sottorete calcola il numero di bit che verranno usati per la sottorete
      Verranno utilizzati più bit possibili*/
    private int[] calculateBitsForSubnet(){
        int[] array = new int[subnets];
        for(int i=0; i<subnets; i++){
            boolean stop=false;
            int numberOfBits=0;
            while(!stop){
                
                if(Math.pow(2, numberOfBits) >= hosts[i] + 3){
                    stop=true;
                    array[i] = (hostBytes*8) - numberOfBits;
                }
                numberOfBits++;
            }
        }
        return array;
    }
    
    //calcola il numero di bit per gli hosts di ogni sottorete
    private int[] calculateBitsForHosts(){
        int[] array = new int[subnets];
        for(int i=0; i<subnets; i++){
            array[i] = (hostBytes*8) - bitsNeededForSubnets[i];
        }
        
        return array;
    }
    
    //calcola il numero di byte di rete e host dell'indirizzo
    private void calculateNetAndHostBytes(){
        switch(ipClass){
            case 'A' -> netBytes = 1;
            case 'B' -> netBytes = 2;
            default -> netBytes = 3;
        }
        hostBytes = 4-netBytes;
    }
    
    //calcola la Stringa della maschera e i byte della maschera
    private void calculateDefaultMask(){
        switch(ipClass){
            case 'A' ->{
                netmaskBytes[0] = 255;
                netmaskBytes[1] = 0;
                netmaskBytes[2] = 0;
                netmaskBytes[3] = 0;
                
                netmask=netmaskBytes[0]+"."+netmaskBytes[1]+"."+netmaskBytes[2]+"."+netmaskBytes[3];
            }
            case 'B' ->{
                netmaskBytes[0] = 255;
                netmaskBytes[1] = 255;
                netmaskBytes[2] = 0;
                netmaskBytes[3] = 0;
                
                netmask=netmaskBytes[0]+"."+netmaskBytes[1]+"."+netmaskBytes[2]+"."+netmaskBytes[3];
            }
            default ->{
                netmaskBytes[0] = 255;
                netmaskBytes[1] = 0;
                netmaskBytes[2] = 0;
                netmaskBytes[3] = 0;
                
                netmask=netmaskBytes[0]+"."+netmaskBytes[1]+"."+netmaskBytes[2]+"."+netmaskBytes[3];
            }
        }
    }
    
    private void calculateNetmask(){
        String[] bites = {"", "", "", ""};
        
        for(int i=0; i<32; i++){
            bites[i/8] += (i<prefixLength)? 1:0;
        }
        
        for(int i=0; i<4; i++){
            netmaskBytes[i] = Integer.parseInt(bites[i], 2);
        }
        
        netmask = netmaskBytes[0] + "." + netmaskBytes[1] + "." + netmaskBytes[2] + "." + netmaskBytes[3];
        
    }
        
    private char calculateAddressClass(){
        if(bytes[0] < 128)
            return 'A';
        else if(bytes[0] < 192)
            return 'B';
        else 
            return 'C';
    }
    
    private boolean isPrivateAddress(){
        return (bytes[0] == 10) || 
               (bytes[0] == 172 && (bytes[1] >= 16 && bytes[1] <= 31)) ||
               (bytes[0] == 192 && bytes[1] == 168);
    }
    
    private int calculatePrefixLength(){
        int prefix=0;
        boolean stop=false;
        
        for(int i=0; !stop && i<4; i++){
            if(netmaskBytes[i] == 255){
                    prefix+=8;
            }else{
                String binaryValue = Integer.toBinaryString(netmaskBytes[i]);
                for(int j=0; !stop && j<8; j++){
                    if(binaryValue.charAt(j) == '1')
                        prefix++;
                    else
                       stop=true; 
                }
            }
        }
        return prefix;
    }
        
    
    
    private String addNetBytes(){
        String address="";
        //aggiunge ad ogni indirizzo i byte di rete (che sono invariati per ogni sottorete)
        for(int k=0; k<netBytes; k++){
            address += bytes[k] + ".";
        }
        return address;
    }
    
    //ritorna l'indirizzo successivo a quello passato da parametro
    private String incrementAddress(String addr){
        int[] bytes = turnIntoBytes(addr);
        
        bytes[3] = (bytes[3]+1)%256;
        bytes[2] = (bytes[2] + ((bytes[3] == 0)? 1 : 0)) % 256;
        bytes[1] = (bytes[1] + ((bytes[2] == 0)? 1 : 0)) % 256;
        
        return bytesToString(bytes);
    }
    
    //ritorna l'indirizzo precedente di quello passato da parametro
    private String decrementAddress(String addr){
        int[] bytes = turnIntoBytes(addr);
        
        bytes[3] = (bytes[3]-1 < 0)? 255 : bytes[3]-1;
        bytes[2] = ((bytes[3] == 255)? ((bytes[2]-1 < 0)? 255 : bytes[2]-1) : bytes[2]); 
        bytes[1] = ((bytes[2] == 255)? bytes[1]-1 : bytes[1]); 
        
        return bytesToString(bytes);
    }
    
    //ritorna un array di interi che rappresenta i byte di un indirizzo passato da parametro
    private int[] turnIntoBytes(String s){
        int[] arr = new int[4];
        int lastInd=0;
        int ind = s.indexOf('.');
        
        for(int i=0; i<4; i++){
            arr[i] = Integer.parseInt(s.substring(lastInd, ind));
            
            //prima lettera dopo il punto precendente
            lastInd = ind + 1;
            
            //trova l'indice del punto successivo nell'indirizzo
            int ipIndexOf = s.indexOf('.', lastInd);
            ind = (ipIndexOf != -1)? ipIndexOf : s.length();
        }
        return arr;  
    }
    
    private String bytesToString(int[] bytes){
        return bytes[0] + "." + bytes[1] + "." + bytes[2] + "." + bytes[3];
    }
    
    private String invertString(String s){
        String inv="";
        
        if(s.length() != 0){
            for(int i=s.length()-1; i>=0; i--){
                inv += s.charAt(i);
            }
        }
        
        return inv;
    }
    
    private String getAddresses(int index){
        String s="";
        for(int i=0; i<addresses.length; i++){
            s+=addresses[i][index] + " ";
        }
        return s;
    }
    
    private String returnMatrix(String[][] matrix){
        String s="";
        for(int i=0; i<matrix.length; i++){
            for(int j=0; j<matrix[i].length; j++){
                s += matrix[i][j] + " ";
            }
            s+="  ";
        }
        
        
        return s;
    }
    
    private String getAddressPerSubnet(int subnetIndex){
        String s="";
        
        s += "Indirizzo di rete: " + addresses[subnetIndex][1];
        s += "\nIndirizzo di broadcast: " + addresses[subnetIndex][0];
        s += "\nIndirizzo del primo host: " + addresses[subnetIndex][2];
        s += "\nIndirizzo dell'ultimo host: " + addresses[subnetIndex][3];
        s += "\nIndirizzo del primo host libero: " + addresses[subnetIndex][4];
        s += "\nIndirizzo dell'ultimo host libero: " + addresses[subnetIndex][5];
        s += "\nDefault Gateway: " + addresses[subnetIndex][6];
        
        return s;
    }
    
    @Override
    public String toString(){
        return  "\n----INFORMAZIONI----" +
                "\nIndirizzo: " + ip +
                "\nClasse: " + ipClass +
                "\nE' un indirizzo " + (isPrivate? "privato" : "pubblico") +
                "\nPrefix length: " + prefixLength +
                "\nMaschera di rete: " + netmask +
                "\nNumero di bit per la sottorete: " + Arrays.toString(bitsNeededForSubnets) +
                "\nBit di sottorete: " + Arrays.toString(subnetBits) +
                "\nMaschere di sottorete: " + Arrays.toString(subnetMask) +
                "\nIndirizzi broadcast: " + getAddresses(0) +
                "\nIndirizzi di rete: " + getAddresses(1) +
                "\nIndirizzi del primo host: " + getAddresses(2) +
                "\nIndirizzi dell'ultimo host: " + getAddresses(3) +
                "\nIndirizzi del primo host libero: " + getAddresses(4) +
                "\nIndirizzi dell'ultimo host libero: " + getAddresses(5) +
                "\nIndirizzi del default gateway: " + getAddresses(6) +
                "\nMatrice dei bit di sottorete: " + returnMatrix(subnetBitsPerByte) +
                "\nMatrice dei bit degli host: " + returnMatrix(hostBitsPerByte) +
                "\nHosts in binario: " + Arrays.toString(hostsInBinary)
                ;
    }
    
    public String getResults(){
        String s="";
        
        for(int i=0; i<subnets; i++){
            s += (i+1) + "^ Sottorete :\n";
            s += getAddressPerSubnet(i) + "\n\n";
        }
        
        return s;
    }
}
