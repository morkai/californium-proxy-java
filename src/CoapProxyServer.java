import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.util.Log;

import naga.ConnectionAcceptor;
import naga.NIOServerSocket;
import naga.NIOService;
import naga.NIOSocket;
import naga.ServerSocketObserver;

public class CoapProxyServer implements ServerSocketObserver
{
  public static final int PACKET_HEADER_LENGTH = 4;
  
  private static final int DEFAULT_PORT = 1337;
  
  private static final Level DEFAULT_LOG_LEVEL = Level.ALL;
  
  private static final Logger LOGGER = Logger.getLogger(CoapProxyServer.class.getName());

  public static void main(String[] args)
  {
    Level logLevel = DEFAULT_LOG_LEVEL;
    int port = DEFAULT_PORT;
    
    for (int i = 0, l = args.length; i < l; i += 2)
    {
      String propertyName = args[i];
      
      if (i + 1 == l)
      {
        break;
      }
      
      String propertyValue = args[i + 1];
      
      switch (propertyName)
      {
        case "--port":
          port = Integer.parseInt(propertyValue);
          break;
        
        case "--log":
          logLevel = Level.parse(propertyValue.toUpperCase());
          break;
      }
    }
    
    LOGGER.setLevel(logLevel);
    Logger.getLogger(CoapProxySocketObserver.class.getName()).setLevel(logLevel);
    Log.setLevel(logLevel);
    Log.init();
    
    try
    {
      NIOService service = new NIOService();
      NIOServerSocket socket = service.openServerSocket(port);
      
      socket.listen(new CoapProxyServer());
      socket.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
      
      LOGGER.info("Listening on port " + port + ".");
      
      while (true)
      {
        service.selectBlocking();
      }
    }
    catch (Exception x)
    {
      x.printStackTrace();
      System.exit(1);
    }
  }
  
  public static byte[] createPacket(int requestId, Message coapMessage)
  {
    byte[] coapFrame = coapMessage.toByteArray();
    byte[] packet = new byte[2 + coapFrame.length];
    
    packet[0] = (byte)((requestId & 0xFF00) >> 8);
    packet[1] = (byte)(requestId & 0x00FF);
    
    System.arraycopy(coapFrame, 0, packet, 2, coapFrame.length);
    
    return packet;
  }
  
  public static String hex(byte[] data)
  {
    if (data != null && data.length!=0)
    {
      StringBuilder builder = new StringBuilder(data.length * 3);
      
      for (int i = 0; i < data.length; i++)
      {
        builder.append(String.format("%02X", (0xFF & data[i])));
        
        if (i < data.length - 1)
        {
          builder.append(' ');
        }
      }
      
      return builder.toString();
    }
    else
    {
      return "--";
    }
  }
  
  @Override
  public void acceptFailed(IOException x)
  {
    LOGGER.warning("Failed to accept a connection: " + x);
  }

  @Override
  public void serverSocketDied(Exception x)
  {
    LOGGER.severe("Server socket died: " + x);
    System.exit(1);
  }

  @Override
  public void newConnection(NIOSocket socket)
  {
    new CoapProxySocketObserver(socket);
  }
}
