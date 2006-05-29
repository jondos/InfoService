/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.client;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.security.SignatureException;
import java.security.InvalidKeyException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.ErrorCodes;
import anon.client.crypto.ASymCipher;
import anon.client.crypto.KeyPool;
import anon.client.crypto.SymCipher;
import anon.crypto.JAPCertificate;
import anon.crypto.JAPSignature;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLEncryption;
import anon.crypto.XMLSignature;
import anon.util.Base64;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


/**
 * @author Stefan Lieske
 */
public class KeyExchangeManager {

  /**
   * Stores the lock on the certificate used by the mixcascade to sign all
   * cascade related messages, like the MixCascade or MixCascadeStatus
   * structures. The certificate will be stored within the signature
   * verification certificate store until the lock is released (done when the
   * connection to the mixcascade is closed).
   */
  private int m_mixCascadeCertificateLock;

  private Object m_internalSynchronization;

  private boolean m_protocolWithTimestamp;

  private boolean m_paymentRequired;

  private SymCipher m_firstMixSymmetricCipher;

  private boolean m_chainProtocolWithFlowControl;

  private FixedRatioChannelsDescription m_fixedRatioChannelsDescription;

  private MixParameters[] m_mixParameters;

  private SymCipher m_multiplexerInputStreamCipher;

  private SymCipher m_multiplexerOutputStreamCipher;

  /**
   * @todo allow to connect if one or more mixes (user specified) cannot be verified
   * @param a_inputStream InputStream
   * @param a_outputStream OutputStream
   * @throws XMLParseException
   * @throws SignatureException
   * @throws IOException
   * @throws UnknownProtocolVersionException
   */
  public KeyExchangeManager(InputStream a_inputStream, OutputStream a_outputStream)
	  throws XMLParseException, SignatureException, IOException, UnknownProtocolVersionException {
    try {
      m_mixCascadeCertificateLock = -1;
      m_internalSynchronization = new Object();
      DataInputStream dataStreamFromMix = new DataInputStream(a_inputStream);
      /* read the length of the following XML structure */
      int xmlDataLength = dataStreamFromMix.readUnsignedShort();
      /* read the initial XML structure */
      byte[] xmlData = new byte[xmlDataLength];
      while (xmlDataLength > 0) {
        int bytesRead = a_inputStream.read(xmlData, xmlData.length - xmlDataLength, xmlDataLength);
        if (bytesRead == -1) {
          throw new EOFException("EOF detected while reading initial XML structure.");
        }
        else {
          xmlDataLength = xmlDataLength - bytesRead;
        }
      }
      /* process the received XML structure */
      Document doc = XMLUtil.toXMLDocument(xmlData);
      Element mixCascadeNode = doc.getDocumentElement();
      if (mixCascadeNode == null) {
        throw (new XMLParseException(XMLParseException.ROOT_TAG, "No document element in received XML structure."));
      }
      if (!mixCascadeNode.getNodeName().equals("MixCascade")) {
        throw (new XMLParseException(XMLParseException.ROOT_TAG, "MixCascade node expected in received XML structure."));
      }
      /* verify the signature */
      if (SignatureVerifier.getInstance().verifyXml(mixCascadeNode, SignatureVerifier.DOCUMENT_CLASS_MIX) == false) {
        throw (new SignatureException("Received XML structure has an invalid signature."));
      }
      /*
       * get the appended certificate of the signature and store it in the
       * certificate store (needed for verification of the MixCascadeStatus
       * messages)
       */
      JAPCertificate firstMixCertificate = null;
      try {
        XMLSignature documentSignature = XMLSignature.getUnverified(mixCascadeNode);
        Enumeration appendedCertificates = documentSignature.getCertificates().elements();
        /*
         * add the first certificate (there should be only one) to the
         * certificate store for later verification of MixCascadeStatus messages
         */
        if (appendedCertificates.hasMoreElements()) {
          firstMixCertificate = (JAPCertificate)(appendedCertificates.nextElement());
          m_mixCascadeCertificateLock = SignatureVerifier.getInstance().getVerificationCertificateStore().addCertificateWithVerification(firstMixCertificate, JAPCertificate.CERTIFICATE_TYPE_MIX, false);
          LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Added appended certificate from the MixCascade structure to the certificate store.");
        }
        else {
          LogHolder.log(LogLevel.DEBUG, LogType.MISC, "No appended certificates in the MixCascade structure.");
        }
      }
      catch (Exception e) {
        LogHolder.log(LogLevel.ERR, LogType.MISC, "Error while looking for appended certificates in the MixCascade structure: " + e.toString());
      }
      /* get the used channel protocol version */
      NodeList channelMixProtocolVersionNodes = mixCascadeNode.getElementsByTagName("MixProtocolVersion");
      if (channelMixProtocolVersionNodes.getLength() == 0) {
        throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "MixProtocolVersion (channel) node expected in received XML structure."));
      }
      /* there should be only one channel mix protocol version node */
      Element channelMixProtocolVersionNode = (Element)(channelMixProtocolVersionNodes.item(0));
      String channelMixProtocolVersionValue = XMLUtil.parseValue(channelMixProtocolVersionNode, null);
      if (channelMixProtocolVersionValue == null) {
        throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
									 "MixProtocolVersion (channel) node has no value."));
      }
      channelMixProtocolVersionValue = channelMixProtocolVersionValue.trim();
      m_protocolWithTimestamp = false;
      m_paymentRequired = false;
      m_firstMixSymmetricCipher = null;
      /*
       * lower protocol versions not listed here are obsolete and not supported
       * any more
       */
      LogHolder.log(LogLevel.DEBUG, LogType.NET,
					"Cascade is using channel-protocol version '" + channelMixProtocolVersionValue + "'.");
      if (channelMixProtocolVersionValue.equals("0.2")) {
        /* no modifications of the default-settings required */
      }
      else if (channelMixProtocolVersionValue.equals("0.4")) {
        m_firstMixSymmetricCipher = new SymCipher();
      }
      else if (channelMixProtocolVersionValue.equals("0.8")) {
        m_protocolWithTimestamp = true;
        m_firstMixSymmetricCipher = new SymCipher();
      }
      else if (channelMixProtocolVersionValue.equalsIgnoreCase("0.9")) {
        m_firstMixSymmetricCipher = new SymCipher();
        m_paymentRequired = true;
      }
      else {
        throw (new UnknownProtocolVersionException(
			  "Unknown channel protocol version used ('" + channelMixProtocolVersionValue + "')."));
      }
      /* get the information about the mixes in the cascade */
      NodeList mixesNodes = mixCascadeNode.getElementsByTagName("Mixes");
      if (mixesNodes.getLength() == 0) {
        throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
									 "Mixes node expected in received XML structure."));
      }
      /* there should be only one mixes node */
      Element mixesNode = (Element)(mixesNodes.item(0));
      NodeList mixNodes = mixesNode.getElementsByTagName("Mix");
      if (mixNodes.getLength() == 0) {
        throw (new XMLParseException("No information about mixes found in the received XML structure."));
      }
      m_mixParameters = new MixParameters[mixNodes.getLength()];
      for (int i = 0; i < mixNodes.getLength(); i++) {
        Element currentMixNode = (Element)(mixNodes.item(i));
        if (i > 0) {
          /*
           * not the first mix --> we have to check the signature (first mix'
           * signature is checked with the MixCascade node)
           */
          if (!SignatureVerifier.getInstance().verifyXml(currentMixNode, SignatureVerifier.DOCUMENT_CLASS_MIX)) {
            throw (new SignatureException("Received XML structure has an invalid signature for Mix " + Integer.toString(i) + "."));
          }
        }
        /* get the mix parameters */
        String currentMixId = XMLUtil.parseAttribute(currentMixNode, "id", null);
        if (currentMixId == null) {
          throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
									   "XML structure of Mix " + Integer.toString(i) + " does not contain a Mix-ID."));
        }
        m_mixParameters[i] = new MixParameters(currentMixId, new ASymCipher());
        if (m_mixParameters[i].getMixCipher().setPublicKey(currentMixNode) != ErrorCodes.E_SUCCESS) {
          throw (new XMLParseException("Received XML structure contains an invalid public key for Mix " + Integer.toString(i) + "."));
        }
        if (i == (mixNodes.getLength() - 1)) {
          /* get the chain protocol version from the last mix */
          NodeList chainMixProtocolVersionNodes = currentMixNode.getElementsByTagName("MixProtocolVersion");
          if (chainMixProtocolVersionNodes.getLength() == 0) {
            throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
										 "MixProtocolVersion (chain) node expected in received XML structure."));
          }
          /* there should be only one chain mix protocol version node */
          Element chainMixProtocolVersionNode = (Element)(chainMixProtocolVersionNodes.item(0));
          String chainMixProtocolVersionValue = XMLUtil.parseValue(chainMixProtocolVersionNode, null);
          if (chainMixProtocolVersionValue == null) {
            throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
										 "MixProtocolVersion (chain) node has no value."));
          }
          chainMixProtocolVersionValue = chainMixProtocolVersionValue.trim();
          m_chainProtocolWithFlowControl = false;
          m_fixedRatioChannelsDescription = null;
          /*
           * lower protocol versions not listed here are obsolete and not
           * supported any more
           */
          LogHolder.log(LogLevel.DEBUG, LogType.NET,
						"Cascade is using chain-protocol version '" + chainMixProtocolVersionValue + "'.");
          if (chainMixProtocolVersionValue.equals("0.3")) {
            /* no modification of the default settings required */
          }
          else if (chainMixProtocolVersionValue.equals("0.4")) {
            m_chainProtocolWithFlowControl = true;
          }
          else if (chainMixProtocolVersionValue.equals("0.5")) {
            /* simulated 1:n channels */
            NodeList downstreamPacketsNodes = currentMixNode.getElementsByTagName("DownstreamPackets");
            if (downstreamPacketsNodes.getLength() == 0) {
              throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
										   "DownstreamPackets node expected in received XML structure."));
            }
            /* there should be only one downstream packets node */
            Element downstreamPacketsNode = (Element)(downstreamPacketsNodes.item(0));
            int downstreamPackets = XMLUtil.parseValue(downstreamPacketsNode, -1);
            if (downstreamPackets < 1) {
              throw (new XMLParseException("DownstreamPackets node has an invalid value."));
            }
            NodeList channelTimeoutNodes = currentMixNode.getElementsByTagName("ChannelTimeout");
            if (channelTimeoutNodes.getLength() == 0) {
              throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "KeyExchangeManager: Constructor: ChannelTimeout node expected in received XML structure."));
            }
            /* there should be only one channel timeout node */
            Element channelTimeoutNode = (Element)(channelTimeoutNodes.item(0));
            long channelTimeout = XMLUtil.parseValue(channelTimeoutNode, -1);
            if (channelTimeout < 1) {
              throw (new XMLParseException("ChannelTimeout node has an invalid value."));
            }
            channelTimeout = 1000L * channelTimeout;
            NodeList chainTimeoutNodes = currentMixNode.getElementsByTagName("ChainTimeout");
            if (chainTimeoutNodes.getLength() == 0) {
              throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "KeyExchangeManager: Constructor: ChainTimeout node expected in received XML structure."));
            }
            /* there should be only one chain timeout node */
            Element chainTimeoutNode = (Element)(chainTimeoutNodes.item(0));
            long chainTimeout = XMLUtil.parseValue(chainTimeoutNode, -1);
            if (chainTimeout < 1) {
              throw (new XMLParseException("ChainTimeout node has an invalid value."));
            }
            chainTimeout = 1000L * chainTimeout;
            m_fixedRatioChannelsDescription = new FixedRatioChannelsDescription(downstreamPackets, channelTimeout, chainTimeout);
          }
          else {
            throw (new UnknownProtocolVersionException(
				  "Unknown chain protocol version used ('" + chainMixProtocolVersionValue + "')."));
          }
        }
      }
      /* sending symmetric keys for multiplexer stream encryption */
      m_multiplexerInputStreamCipher = new SymCipher();
      m_multiplexerOutputStreamCipher = new SymCipher();
      /* ensure that keypool is started */
      KeyPool.start();
      if (m_firstMixSymmetricCipher == null) {
        /*
         * create a new MixPacket with the keys (channel id and flags doesn't
         * matter)
         */
        MixPacket keyPacket = new MixPacket(0);
        byte[] keyPacketIdentifier = "KEYPACKET".getBytes();
        System.arraycopy(keyPacketIdentifier, 0, keyPacket.getPayloadData(), 0, keyPacketIdentifier.length);
        byte[] keyBuffer = new byte[32];
        KeyPool.getKey(keyBuffer, 0);
        KeyPool.getKey(keyBuffer, 16);
        System.arraycopy(keyBuffer, 0, keyPacket.getPayloadData(), keyPacketIdentifier.length, keyBuffer.length);
        m_mixParameters[0].getMixCipher().encrypt(keyPacket.getPayloadData(), 0, keyPacket.getPayloadData(), 0);
        a_outputStream.write(keyPacket.getRawPacket());
        m_multiplexerInputStreamCipher.setEncryptionKeyAES(keyBuffer, 0, 16);
        m_multiplexerOutputStreamCipher.setEncryptionKeyAES(keyBuffer, 16, 16);
      }
      else {
        /*
         * the first mix uses a symmetric cipher for mixing -> send also keys
         * for that cipher
         */
        Document keyDoc = XMLUtil.createDocument();
        if (keyDoc == null) {
          throw (new XMLParseException("Cannot create XML document for key exchange."));
        }
        Element japKeyExchangeNode = keyDoc.createElement("JAPKeyExchange");
        japKeyExchangeNode.setAttribute("version", "0.1");
        Element linkEncryptionNode = keyDoc.createElement("LinkEncryption");
        byte[] multiplexerKeys = new byte[64];
        KeyPool.getKey(multiplexerKeys, 0);
        KeyPool.getKey(multiplexerKeys, 16);
        KeyPool.getKey(multiplexerKeys, 32);
        KeyPool.getKey(multiplexerKeys, 48);
        m_multiplexerOutputStreamCipher.setEncryptionKeyAES(multiplexerKeys, 0, 32);
        m_multiplexerInputStreamCipher.setEncryptionKeyAES(multiplexerKeys, 32, 32);
        XMLUtil.setValue(linkEncryptionNode, Base64.encode(multiplexerKeys, true));
        japKeyExchangeNode.appendChild(linkEncryptionNode);
        Element mixEncryptionNode = keyDoc.createElement("MixEncryption");
        byte[] mixKeys = new byte[32];
        KeyPool.getKey(mixKeys, 0);
        KeyPool.getKey(mixKeys, 16);
        m_firstMixSymmetricCipher.setEncryptionKeyAES(mixKeys, 0, 32);
        XMLUtil.setValue(mixEncryptionNode, Base64.encode(mixKeys, true));
        japKeyExchangeNode.appendChild(mixEncryptionNode);
        keyDoc.appendChild(japKeyExchangeNode);
        XMLEncryption.encryptElement(japKeyExchangeNode, m_mixParameters[0].getMixCipher().getPublicKey());
        ByteArrayOutputStream keyExchangeBuffer = new ByteArrayOutputStream();
        byte[] keyExchangeXmlData = XMLUtil.toByteArray(keyDoc);
        DataOutputStream keyExchangeDataStream = new DataOutputStream(keyExchangeBuffer);
        keyExchangeDataStream.writeShort(keyExchangeXmlData.length);
        keyExchangeDataStream.flush();
        keyExchangeBuffer.write(keyExchangeXmlData);
        keyExchangeBuffer.flush();
        byte[] keyExchangeData = keyExchangeBuffer.toByteArray();
        a_outputStream.write(keyExchangeData);
        a_outputStream.flush();
        /*
         * now receive and check the signature responded from the mix -> this
         * doesn't much sense because if the mix uses other keys, it cannot
         * decrypt our messages and we cannot decrypt messages from the mix ->
         * this is only a denial-of-service attack and an attacker who is able
         * to modify the keys (he cannot read them because they are crypted with
         * the public key of the mix which was signed by the mix with a
         * signature verified against an internal certificate) is also able to
         * modify every single packet
         */

        /*
         * TODO: It's very nasty to use the old raw signature implementation. It
         * should be rewritten in the next version of the key exchange protocol
         * (if a signature is still used there).
         */
        int keySignatureXmlDataLength = dataStreamFromMix.readUnsignedShort();
        byte[] keySignatureXmlData = new byte[keySignatureXmlDataLength];
        while (keySignatureXmlDataLength > 0) {
          int bytesRead = a_inputStream.read(keySignatureXmlData, keySignatureXmlData.length - keySignatureXmlDataLength, keySignatureXmlDataLength);
          if (bytesRead == -1) {
            throw new EOFException("EOF detected while reading symmetric key signature XML structure.");
          }
          else {
            keySignatureXmlDataLength = keySignatureXmlDataLength - bytesRead;
          }
        }
        Document keySignatureDoc = XMLUtil.toXMLDocument(keySignatureXmlData);
        Element keySignatureNode = keySignatureDoc.getDocumentElement();
        if (keySignatureNode == null) {
          throw (new XMLParseException(XMLParseException.ROOT_TAG, "No document element in received symmetric key signature XML structure."));
        }
        if (!keySignatureNode.getNodeName().equals("Signature")) {
          throw (new XMLParseException(XMLParseException.ROOT_TAG, "Signature node expected in received symmetric key signature XML structure."));
        }
        NodeList keySignatureValueNodes = keySignatureNode.getElementsByTagName("SignatureValue");
        if (keySignatureValueNodes.getLength() == 0) {
          throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "SignatureValue node expected in received symmetric key signature XML structure."));
        }
        /* there should be only one SignatureValue node */
        Element keySignatureValueNode = (Element)(keySignatureValueNodes.item(0));
        String keySignatureValue = XMLUtil.parseValue(keySignatureValueNode, null);
        if (keySignatureValue == null) {
          throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "SignatureValue node in symmetric key signature XML structure has no value."));
        }
        byte[] keySignatureData = Base64.decode(keySignatureValue);
        if (keySignatureData == null) {
          throw (new SignatureException(
				"SignatureValue node in symmetric key signature XML structure has an invalid value."));
        }
        /*
         * now take the certificate of the first mix from the origin MixCascade
         * structure and check the received signature of the symmetric keys
         * against that certificate
         */
        JAPSignature signatureVerificationInstance = new JAPSignature();
		try
		{
			signatureVerificationInstance.initVerify(firstMixCertificate.getPublicKey());
		}
		catch (InvalidKeyException a_e)
		{
			throw new SignatureException(a_e.getMessage());
		}
        if (!signatureVerificationInstance.verify(keyExchangeData, keySignatureData, true)) {
          throw (new SignatureException("Invalid symmetric keys signature received."));
        }
      }
    }
    catch (SignatureException e) {
      /* clean up */
      removeCertificateLock();
      throw e;
    }
  }

  public boolean isProtocolWithTimestamp() {
    return m_protocolWithTimestamp;
  }

  public boolean isPaymentRequired() {
    return m_paymentRequired;
  }

  public boolean isChainProtocolWithFlowControl() {
    return m_chainProtocolWithFlowControl;
  }

  public FixedRatioChannelsDescription getFixedRatioChannelsDescription() {
    return m_fixedRatioChannelsDescription;
  }

  public SymCipher getFirstMixSymmetricCipher() {
    return m_firstMixSymmetricCipher;
  }

  public SymCipher getMultiplexerInputStreamCipher() {
    return m_multiplexerInputStreamCipher;
  }

  public SymCipher getMultiplexerOutputStreamCipher() {
    return m_multiplexerOutputStreamCipher;
  }

  public MixParameters[] getMixParameters() {
    return m_mixParameters;
  }

  public void removeCertificateLock() {
    synchronized (m_internalSynchronization) {
      if (m_mixCascadeCertificateLock != -1) {
        SignatureVerifier.getInstance().getVerificationCertificateStore().removeCertificateLock(m_mixCascadeCertificateLock);
        m_mixCascadeCertificateLock = -1;
      }
    }
  }

}
