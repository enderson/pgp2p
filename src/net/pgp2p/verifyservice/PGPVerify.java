package net.pgp2p.verifyservice;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.pgp2p.cryptoservice.PGPManager;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSignature;

public class PGPVerify {
	
	/**
	 * Logger for this class
	 */
	private final static Logger logger = Logger.getLogger(PGPVerify.class.getName()); 
	
	public PGPManager manager;

	public PGPVerify(String path) {
		try {
			//this.manager = PGPManager.getInstance(path);
			this.manager = new PGPManager(path);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PGPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public PGPVerify(PGPManager manager) {
		this.manager = manager;
	}

	/**
	 * Verifies if a given key is known and trusted in the keyring of the PGPManager.
	 * 
	 * @param PGPPublicKey pubKey
	 * @return boolean - true if the keys is known and trusted, false otherwise 
	 * @throws PGPException
	 */
	public boolean isTrusted(PGPPublicKey pubKey) throws PGPException {
		//TODO - considerar o n�vel de confian�a
		
		// Captura a publicKey fornecida como parametro na base local 
		PGPPublicKey pubKeyToVerify = manager.publicKeyRing.getPublicKey(pubKey.getKeyID());
		
		if ( pubKeyToVerify == null) {
			logger.log(Level.INFO, "A chave "+ Long.toHexString(pubKey.getKeyID()).toUpperCase().substring(8, 16) + " n�o existe na base local.");
			return false;
		} else {
			Iterator<PGPSignature> sigs = pubKeyToVerify.getSignatures();
			PGPSignature sign = null;
			
			while (sigs.hasNext()) {
				sign = sigs.next();
				if (sign.getKeyID() == manager.getPublicKey().getKeyID()) {
					logger.log(Level.INFO, manager.getUserID() + " CONFIA (tem a pubKey e assinou) em " + pubKey.getUserIDs().next());
					return true;
				}
			}

		}
		return false;
	}
	
	
	/**
	 * Receives an ASCII armored pubkey for validation, and than invoke the isTrusted(PGPPubKey).
	 *   
	 * @param String armoredPubKey
	 * @return boolean - true if the keys is known and trusted, false otherwise
	 * @throws IOException
	 * @throws PGPException
	 */
	public boolean isTrusted(String armoredPubKey) throws IOException, PGPException {	
		PGPPublicKey 		pubKey		= PGPManager.getPublicKey(armoredPubKey); 

		return isTrusted(pubKey); 
	}

	
}
