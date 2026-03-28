package j3d.sound;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory;

/**
 * Utility class for loading and playing OGG Vorbis sound files using LWJGL's
 * STBVorbis and OpenAL.
 */
public class OggSoundLoader {

    private int bufferId;
    private int sourceId;

    /**
     * Loads an OGG Vorbis file from the given path into an OpenAL buffer.
     *
     * @param filePath The path to the OGG file.
     * @return An integer ID representing the OpenAL buffer. Returns 0 if loading
     *         fails.
     */
    public static int loadOggSound(String filePath) {
        ByteBuffer oggByteBuffer;
        try {
            oggByteBuffer = ioResourceToByteBuffer(filePath, 1024 * 1024); // Allocate 1MB initially
        } catch (IOException e) {
            System.err.println("Failed to load OGG file: " + filePath + " - " + e.getMessage());
            return 0;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);

            ShortBuffer pcm = stb_vorbis_decode_memory(oggByteBuffer, channels, sampleRate);

            if (pcm == null) {
                System.err.println("Failed to decode OGG Vorbis file: " + filePath);
                return 0;
            }

            int format = -1;
            if (channels.get(0) == 1) {
                format = AL_FORMAT_MONO16;
            } else if (channels.get(0) == 2) {
                format = AL_FORMAT_STEREO16;
            }

            if (format == -1) {
                System.err.println("Unsupported number of channels for OGG file: " + filePath);
                return 0;
            }

            int bufferId = alGenBuffers();
            alBufferData(bufferId, format, pcm, sampleRate.get(0));

            // Importante: pcm é alocado nativamente e deve ser liberado manualmente
            MemoryUtil.memFree(pcm);

            return bufferId;
        }
    }

    /**
     * Creates an OpenAL source for playing a sound.
     *
     * @param bufferId The ID of the OpenAL buffer containing the sound data.
     * @param loop     Whether the sound should loop.
     * @return An integer ID representing the OpenAL source. Returns 0 if creation
     *         fails.
     */
    public static int createSoundSource(int bufferId, boolean loop) {
        int sourceId = alGenSources();

        alSourcei(sourceId, AL_BUFFER, bufferId);
        alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        alSourcef(sourceId, AL_GAIN, 1.0f); // Default gain
        alSourcef(sourceId, AL_PITCH, 1.0f); // Default pitch

        return sourceId;
    }

    /**
     * Plays a sound from the given OpenAL source.
     *
     * @param sourceId The ID of the OpenAL source to play.
     */
    public static void playSound(int sourceId) {
        alSourcePlay(sourceId);
    }

    /**
     * Stops a sound from the given OpenAL source.
     *
     * @param sourceId The ID of the OpenAL source to stop.
     */
    public static void stopSound(int sourceId) {
        alSourceStop(sourceId);
    }

    /**
     * Cleans up OpenAL resources (source and buffer).
     *
     * @param sourceId The ID of the OpenAL source.
     * @param bufferId The ID of the OpenAL buffer.
     */
    public static void cleanup(int sourceId, int bufferId) {
        alDeleteSources(sourceId);
        alDeleteBuffers(bufferId);
    }

    /**
     * Reads an I/O resource into a ByteBuffer.
     */
    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer = BufferUtils.createByteBuffer(bufferSize);
        byte[] bytes = Files.readAllBytes(Paths.get(resource));
        buffer.put(bytes).flip();
        return buffer;
    }

    public int getBufferId() {
        return bufferId;
    }

    public int getSourceId() {
        return sourceId;
    }
}