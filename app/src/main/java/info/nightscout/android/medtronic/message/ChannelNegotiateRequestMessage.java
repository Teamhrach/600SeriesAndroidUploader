package info.nightscout.android.medtronic.message;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeoutException;

import info.nightscout.android.USB.UsbHidDriver;
import info.nightscout.android.medtronic.MedtronicCnlSession;
import info.nightscout.android.medtronic.exception.ChecksumException;
import info.nightscout.android.medtronic.exception.EncryptionException;
import info.nightscout.android.medtronic.exception.UnexpectedMessageException;
import info.nightscout.android.medtronic.service.MedtronicCnlService;
import info.nightscout.android.utils.HexDump;

/**
 * Created by lgoedhart on 26/03/2016.
 */
public class ChannelNegotiateRequestMessage extends MedtronicRequestMessage<ChannelNegotiateResponseMessage> {
    private static final String TAG = ChannelNegotiateRequestMessage.class.getSimpleName();

    public ChannelNegotiateRequestMessage(MedtronicCnlSession pumpSession) throws ChecksumException {
        super(CommandType.SEND_MESSAGE, CommandAction.CHANNEL_NEGOTIATE, pumpSession, buildPayload(pumpSession));
    }

    @Override
    public ChannelNegotiateResponseMessage send(UsbHidDriver mDevice) throws IOException, TimeoutException, ChecksumException, EncryptionException, UnexpectedMessageException {
        byte[] payload;

        clearMessage(mDevice, PRESEND_CLEAR_TIMEOUT_MS);
        sendMessage(mDevice);

        payload = readMessage_0x81(mDevice);

        // Don't care what the 0x81 response message is at this stage
        Log.d(TAG, "negotiateChannel: Reading 0x81 message");

        // CNL death with a timeout on close seen after this!
        if (payload.length != 0x27) {
            MedtronicCnlService.cnlChannelNegotiateError++;
            clearMessage(mDevice, ERROR_CLEAR_TIMEOUT_MS);
            Log.e(TAG, "Invalid message received for negotiateChannel 0x81 response");
            throw new UnexpectedMessageException("Invalid message received for negotiateChannel 0x81 response " + HexDump.toHexString(payload));
        }

        Log.d(TAG, "negotiateChannel: Reading 0x80 message");
        // Read the 0x80
        payload = readMessage(mDevice);
        if(payload[0x12] != (byte) 0x80) {
            clearMessage(mDevice, ERROR_CLEAR_TIMEOUT_MS);
            Log.e(TAG, "Invalid message received for negotiateChannel 0x80 response");
            throw new UnexpectedMessageException("Invalid message received for negotiateChannel 0x80 response");
        }

        return this.getResponse(payload);
    }

    /*
        @Override
    public ChannelNegotiateResponseMessage send(UsbHidDriver mDevice) throws IOException, TimeoutException, ChecksumException, EncryptionException, UnexpectedMessageException {

        clearMessage(mDevice, 100, mPumpSession);
        sendMessage(mDevice);

        // Don't care what the 0x81 response message is at this stage
        Log.d(TAG, "negotiateChannel: Reading 0x81 message");
        if (readMessage_0x81(mDevice).length != 0x27) {
            Log.e(TAG, "Invalid message received for negotiateChannel 0x81 response");
            throw new UnexpectedMessageException("Invalid message received for negotiateChannel 0x81 response");
        }

        Log.d(TAG, "negotiateChannel: Reading 0x80 message");
        // Read the 0x80
        byte[] payload = readMessage(mDevice);
        if(payload[0x12] != (byte) 0x80) {
            Log.e(TAG, "Invalid message received for negotiateChannel 0x80 response");
            throw new UnexpectedMessageException("Invalid message received for negotiateChannel 0x80 response");
        }

        // Clear unexpected incoming messages
//        clearMessage(mDevice);

        return this.getResponse(payload);
    }

     */
    @Override
    protected ChannelNegotiateResponseMessage getResponse(byte[] payload) throws ChecksumException, EncryptionException, IOException {
        return new ChannelNegotiateResponseMessage(mPumpSession, payload);
    }

    protected static byte[] buildPayload( MedtronicCnlSession pumpSession ) {
        ByteBuffer payload = ByteBuffer.allocate(26);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        // The MedtronicMessage sequence number is always sent as 1 for this message
        // addendum: when network is joined the send sequence number is 1 (first pump request-response)
        // sequence should then be 2 and increment for ongoing messages
        pumpSession.setMedtronicSequenceNumber((byte) 1);
        payload.put((byte) 1);
        payload.put(pumpSession.getRadioChannel());
        byte[] unknownBytes = {0, 0, 0, 0x07, 0x07, 0, 0, 0x02};
        payload.put(unknownBytes);
        payload.putLong(pumpSession.getLinkMAC());
        payload.putLong(pumpSession.getPumpMAC());

        return payload.array();
    }
}
