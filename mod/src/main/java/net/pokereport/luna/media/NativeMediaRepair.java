package net.pokereport.luna.media;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.Arrays;
import java.util.zip.*;

/** Repairs only the Windows files bundled by the installed official binaries mod. */
public final class NativeMediaRepair {
    private NativeMediaRepair() {}
    public static int repair(Path directory,InputStream archive) throws IOException {
        if(archive==null)throw new IOException("Official FFmpeg archive missing");
        Files.createDirectories(directory);
        int repaired=0,entries=0;long total=0;
        try(var zip=new ZipInputStream(archive)) {
            for(ZipEntry entry;(entry=zip.getNextEntry())!=null;) {
                String name=entry.getName();
                if(entry.isDirectory()||name.contains("/")||name.contains("\\")
                        ||!(name.endsWith(".dll")||name.equals("version.cfg")))throw new IOException("Invalid native archive entry");
                if(++entries>32)throw new IOException("Too many native files");
                var bytes=new ByteArrayOutputStream();byte[] buffer=new byte[65536];
                for(int n;(n=zip.read(buffer))>=0;) {
                    if(n==0)continue;
                    total+=n;if(bytes.size()+n>128*1024*1024||total>256L*1024*1024)throw new IOException("Native archive too large");
                    bytes.write(buffer,0,n);
                }
                byte[] expected=bytes.toByteArray();Path target=directory.resolve(name);
                if(Files.isSymbolicLink(target))throw new IOException("Native file is a symbolic link");
                if(Files.isRegularFile(target)&&Files.size(target)==expected.length
                        &&Arrays.equals(digest(target),digest(expected)))continue;
                Path temporary=Files.createTempFile(directory,"pokereport-native-",".part");
                try {
                    Files.write(temporary,expected);
                    try {Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                    catch(AtomicMoveNotSupportedException e){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
                    repaired++;
                } finally {Files.deleteIfExists(temporary);}
            }
        }
        if(entries==0)throw new IOException("Empty native archive");
        return repaired;
    }
    private static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static byte[] digest(byte[] bytes){return sha().digest(bytes);}
    private static byte[] digest(Path path)throws IOException{
        var hash=sha();try(var in=Files.newInputStream(path)){byte[] b=new byte[65536];for(int n;(n=in.read(b))>=0;)if(n>0)hash.update(b,0,n);}return hash.digest();
    }
}
