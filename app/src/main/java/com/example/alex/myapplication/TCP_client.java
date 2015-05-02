/***
A helper class to send NMEA sentences to XCSOAR via TCP-IP 


***/
private void runTcpClient() {
    try {
        Socket s = new Socket("localhost", TCP_SERVER_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
        //send output msg
        String outMsg = "TCP connecting to " + TCP_SERVER_PORT + System.getProperty("line.separator"); 
        out.write(outMsg);
        out.flush();
        Log.i("TcpClient", "sent: " + outMsg);
        //accept server response
        String inMsg = in.readLine() + System.getProperty("line.separator");
        Log.i("TcpClient", "received: " + inMsg);
        //close connection
        s.close();
    } catch (UnknownHostException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    } 
}
