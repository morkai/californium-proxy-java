import java.io.EOFException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.coap.TimeoutHandler;

import naga.NIOSocket;
import naga.SocketObserver;
import naga.packetreader.RegularPacketReader;
import naga.packetwriter.RegularPacketWriter;

public class CoapProxySocketObserver implements SocketObserver
{
  private static final Logger LOGGER = Logger.getLogger(CoapProxySocketObserver.class.getName());

  private final NIOSocket socket;
  
  private final List<Request> observers = new ArrayList<Request>();
  
  public CoapProxySocketObserver(NIOSocket socket)
  {
    this.socket = socket;
    this.socket.setPacketReader(new RegularPacketReader(CoapProxyServer.PACKET_HEADER_LENGTH, true));
    this.socket.setPacketWriter(new RegularPacketWriter(CoapProxyServer.PACKET_HEADER_LENGTH, true));
    this.socket.listen(this);
  }

  @Override
  public void connectionOpened(NIOSocket socket)
  {
    LOGGER.info("Connection opened: " + socket.getIp());
  }
  
  @Override
  public void connectionBroken(NIOSocket socket, Exception x)
  {
    if (x instanceof EOFException)
    {
      LOGGER.info("Connection closed: " + socket.getIp());
    }
    else
    {
      LOGGER.warning("Connection " + socket.getIp() + " broke: " + x);
    }
  }

  @Override
  public void packetReceived(NIOSocket socket, byte[] data)
  {
    int requestId = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
    byte[] coapFrame = new byte[data.length - 2];
    
    System.arraycopy(data, 2, coapFrame, 0, coapFrame.length);
    
    Request coapRequest = createCoapRequest(coapFrame, requestId);
    
    if (coapRequest == null)
    {
      sendBadRequestResponse(requestId, coapRequest);
      
      return;
    }
    
    if (coapRequest.hasOption(OptionNumberRegistry.OBSERVE))
    {
      observers.add(coapRequest);
    }
    
    coapRequest.send();
  }

  @Override
  public void packetSent(NIOSocket socket, Object tag)
  {
    
  }
  
  public Request createCoapRequest(byte[] coapFrame, int requestId)
  {
    Message coapMessage = Message.fromByteArray(coapFrame);
    
    if (!(coapMessage instanceof Request))
    {
      return null;
    }
    
    Request coapRequest = (Request)coapMessage;
    
    Option proxyUriOption = coapRequest.getFirstOption(OptionNumberRegistry.PROXY_URI);
    String proxyUriString = "coap://127.0.0.1";
    
    if (proxyUriOption != null)
    {
      proxyUriString = proxyUriOption.getStringValue();
      
      coapRequest.removeOptions(OptionNumberRegistry.PROXY_URI);
    }
    
    if (coapRequest.getMID() == 0)
    {
      coapRequest.setMID(-1);
    }
    
    if (!coapRequest.hasOption(OptionNumberRegistry.TOKEN))
    {
      coapRequest.requiresToken(true);
    }
    
    try
    {
      coapRequest.setPeerAddress(new EndpointAddress(new URI(proxyUriString)));
    }
    catch (URISyntaxException x)
    {
      return null;
    }

    coapRequest.registerResponseHandler(createResponseHandler(requestId));
    coapRequest.registerTimeoutHandler(createTimeoutHandler(requestId));
    
    return coapRequest;
  }

  private ResponseHandler createResponseHandler(final int requestId)
  {
    return new ResponseHandler()
    {
      @Override
      public void handleResponse(Response coapResponse)
      {
        Request coapRequest = coapResponse.getRequest();
        
        if (!socket.isOpen())
        {
          if (observers.contains(coapRequest))
          {
            cancelObserver(coapRequest);
            
            observers.remove(coapRequest);
          }
          
          return;
        }

        byte[] packet = CoapProxyServer.createPacket(requestId, coapResponse);
        
        socket.write(packet);
      }
    };
  }
  
  private TimeoutHandler createTimeoutHandler(final int requestId)
  {
    return new TimeoutHandler()
    {
      @Override
      public void handleTimeout(Request coapRequest)
      {
        if (!socket.isOpen())
        {
          return;
        }

        Message coapResponse = coapRequest.newReply(coapRequest.isConfirmable());
        coapResponse.setCode(CodeRegistry.RESP_GATEWAY_TIMEOUT);
        
        byte[] packet = CoapProxyServer.createPacket(requestId, coapResponse);
        
        socket.write(packet);
      }
    };
  }

  private void cancelObserver(Request coapRequest)
  {
    coapRequest.removeOptions(OptionNumberRegistry.OBSERVE);
    coapRequest.setMID(-1);
    coapRequest.send();
  }
  
  private void sendBadRequestResponse(int requestId, Request coapRequest)
  {
    Message coapResponse = coapRequest.newReply(coapRequest.isConfirmable());
    coapResponse.setCode(CodeRegistry.RESP_BAD_REQUEST);
    
    byte[] packet = CoapProxyServer.createPacket(requestId, coapResponse);
    
    socket.write(packet);
  }
}
