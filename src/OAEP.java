import java.security.MessageDigest;
import java.util.Formatter;

public class OAEP {
    private int k = 128, hLen;
    private byte[] message, seed, L;

    public OAEP() {

    }

    public void encrypt(String M, String seed) {
        message = HexToByte(M);
        this.seed = HexToByte(seed);
        String L = "";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        int hLen = md.getDigestLength();

        int mLen = message.length;

        // check length of message
        if (mLen > k - hLen * 2 - 2) {
            System.out.println("message too long");
            System.exit(1);
        }

        byte[] LByte = HexToByte(L);
        md.update(LByte);
        byte[] lHash = md.digest();

        int psLen = k - mLen - hLen * 2 - 2;
        byte[] ps = new byte[psLen];
        byte[] hexVal = {0x01};

        byte[] db = new byte[k - hLen - 1];


        int start = 0;
        System.arraycopy(lHash, 0, db, start, lHash.length);
        start = lHash.length;
        System.arraycopy(ps, 0, db, start, ps.length);
        start += ps.length;
        System.arraycopy(hexVal, 0, db, start, 1);
        start += 1;
        System.arraycopy(message, 0, db, start, message.length);


        MGF1 mgf1 = new MGF1(seed, k - hLen - 1);
        String dbMask = mgf1.getMask();
        byte[] dbMaskByte = HexToByte(dbMask);

        byte[] maskedDB = xOR(db, dbMaskByte);
        String temp = byteToHex(maskedDB);
        MGF1 mgf12 = new MGF1(temp, hLen);
        String seedMask = mgf12.getMask();
        byte[] seedMaskByte = HexToByte(seedMask);
        byte[] maskedSeed = xOR(this.seed, seedMaskByte);

        byte[] hexVal2 = {0x00};
        byte[] EMByte = new byte[1 + maskedSeed.length + maskedDB.length];

        System.arraycopy(hexVal2, 0, EMByte, 0, hexVal2.length);
        System.arraycopy(maskedSeed, 0, EMByte, hexVal2.length, maskedSeed.length);
        System.arraycopy(maskedDB, 0, EMByte, 1 + maskedSeed.length, maskedDB.length);
        System.out.println("REAL: 0000255975c743f5f11ab5e450825d93b52a160aeef9d3778a18b7aa067f90b2178406fa1e1bf77f03f86629dd5607d11b9961707736c2d16e7c668b367890bc6ef1745396404ba7832b1cdfb0388ef601947fc0aff1fd2dcd279dabde9b10bfc51f40e13fb29ed5101dbcb044e6232e6371935c8347286db25c9ee20351ee82");
        System.out.println("OURS: " + byteToHex(EMByte));


    }

    public void decrypt(String EM) {
        byte[] encrypted = HexToByte(EM);
        if (encrypted.length != k) {
            System.out.println("Decryption error");
            System.exit(1);
        }
        if (k < 2 * hLen + 2) {
            System.out.println("Decryption error");
            System.exit(1);
        }
        String empty = "";
        L = HexToByte(empty);

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        md.update(L);
        hLen = md.getDigestLength();
        byte[] lHash = md.digest();

        byte[] y = new byte[1];
        byte[] maskedSeed = new byte[hLen];
        byte[] maskedDB = new byte[k - hLen - 1];

        y[0] = encrypted[0];

        for (int i = 0; i < hLen; i++) {
            maskedSeed[i] = encrypted[i + 1];
        }
        for (int i = 0; i < maskedDB.length; i++) {
            maskedDB[i] = encrypted[i + hLen + 1];
        }

        MGF1 mgf1 = new MGF1(byteToHex(maskedDB), hLen);
        String seedMask = mgf1.getMask();
        byte[] seedMaskByte = HexToByte(seedMask);

        byte[] seed = xOR(maskedSeed, seedMaskByte);

        MGF1 mgf2 = new MGF1(byteToHex(seed), k - hLen - 1);
        String dbMask = mgf2.getMask();
        byte[] dbMaskByte = HexToByte(dbMask);

        byte[] DBByte = xOR(maskedDB, dbMaskByte);

        byte[] message = null;
        int place = 0;
        boolean mBol = false;
        for (int i = hLen - 1; i < DBByte.length; i++) {
            if (mBol == true) {
                message[place] = DBByte[i];
                place++;
            }
            if (DBByte[i] == 0x01 && !mBol) {
                mBol = true;
                message = new byte[DBByte.length - i - 1];
            }

        }


        System.out.println(byteToHex(message));

    }


    private byte[] xOR(byte[] a, byte[] b) {
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ b[i]);
        }
        return out;
    }

    public static byte[] HexToByte(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static String byteToHex(byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
