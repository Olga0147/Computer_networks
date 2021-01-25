package ru.nsu.ccfit.zenina.Lab3;

import ru.nsu.ccfit.zenina.Lab3.Message.Msg;

import java.io.*;

public class Serializer {
    public static byte[] serialize(Msg msg) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                objectOutputStream.writeObject(msg);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static Msg deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                return (Msg) o.readObject();
            }
        }
    }
}