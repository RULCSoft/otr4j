package net.java.otr4j.message.encoded;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.interfaces.DHPublicKey;

import org.bouncycastle.asn1.*;
import net.java.otr4j.Utils;
import net.java.otr4j.crypto.CryptoConstants;
import net.java.otr4j.crypto.CryptoUtils;

public class DeserializationUtils {

	public static PublicKey readPublicKey(ByteArrayInputStream in)
			throws NoSuchAlgorithmException, InvalidKeySpecException,
			IOException {

		int type = DeserializationUtils.readShort(in);
		switch (type) {
		case CryptoConstants.DSA_PUB_TYPE:
			BigInteger p = DeserializationUtils.readMpi(in);
			BigInteger q = DeserializationUtils.readMpi(in);
			BigInteger g = DeserializationUtils.readMpi(in);
			BigInteger y = DeserializationUtils.readMpi(in);
			DSAPublicKeySpec keySpec = new DSAPublicKeySpec(y, p, q, g);
			KeyFactory keyFactory = KeyFactory.getInstance("DSA");
			return keyFactory.generatePublic(keySpec);
		default:
			throw new UnsupportedOperationException();
		}

	}

	public static int readShort(ByteArrayInputStream in) throws IOException {
		byte[] b = new byte[DataLength.SHORT];
		in.read(b);
		return Utils.byteArrayToInt(b);
	}

	public static int readByte(ByteArrayInputStream in) throws IOException {
		byte[] b = new byte[DataLength.BYTE];
		in.read(b);
		return Utils.byteArrayToInt(b);
	}

	static int readDataLen(ByteArrayInputStream in) throws IOException {
		byte[] b = new byte[DataLength.DATALEN];
		in.read(b);
		return Utils.byteArrayToInt(b);
	}

	public static byte[] readData(ByteArrayInputStream in) throws IOException {
		int len = readDataLen(in);

		byte[] b = new byte[len];
		in.read(b);
		return b;
	}

	public static byte[] readMac(ByteArrayInputStream in) throws IOException {
		byte[] b = new byte[DataLength.MAC];
		in.read(b);
		return b;
	}

	static BigInteger readMpi(ByteArrayInputStream in) throws IOException {
		int len = readDataLen(in);

		byte[] b = new byte[len];
		in.read(b);

		return new BigInteger(1, Utils.trim(b));
	}

	public static int readInt(ByteArrayInputStream stream) throws IOException {
		byte[] b = new byte[DataLength.INT];
		stream.read(b);
		return Utils.byteArrayToInt(b);
	}

	public static byte[] readCtr(ByteArrayInputStream in) throws IOException {
		byte[] b = new byte[DataLength.CTR];
		in.read(b);
		return b;
	}

	public static byte[] readSignature(ByteArrayInputStream stream,
			PublicKey pubKey) throws IOException {
		if (!pubKey.getAlgorithm().equals("DSA"))
			throw new UnsupportedOperationException();

		DSAPublicKey dsaPubKey = (DSAPublicKey) pubKey;
		DSAParams dsaParams = dsaPubKey.getParams();
		int qlen = dsaParams.getQ().bitLength() / 8;
		// http://www.codeproject.com/KB/security/CryptoInteropSign.aspx
		// http://java.sun.com/j2se/1.4.2/docs/guide/security/CryptoSpec.html

		byte[] r = new byte[qlen];
		byte[] s = new byte[qlen];

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DERSequenceGenerator seqGen = new DERSequenceGenerator(bos);

		stream.read(r);
		seqGen.addObject(new DERInteger(new BigInteger(1, r)));
		stream.read(s);
		seqGen.addObject(new DERInteger(new BigInteger(1, s)));
		seqGen.close();

		byte[] result = bos.toByteArray();
		bos.close();
		return result;
	}

	static DHPublicKey readDHPublicKey(ByteArrayInputStream in)
			throws IOException {
		BigInteger gyMpi = DeserializationUtils.readMpi(in);
		try {
			return CryptoUtils.getDHPublicKey(gyMpi);
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}
}
