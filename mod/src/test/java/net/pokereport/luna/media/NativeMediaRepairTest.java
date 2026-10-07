package net.pokereport.luna.media;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
class NativeMediaRepairTest {
 @TempDir Path directory;
 private byte[] archive(String... names)throws Exception {
  var bytes=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(bytes)){for(String name:names){zip.putNextEntry(new ZipEntry(name));zip.write((name+"-expected").getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}}return bytes.toByteArray();
 }
 @Test void versionMarkerDoesNotHideMissingOrCorruptDlls()throws Exception {
  byte[] zip=archive("version.cfg","avutil-60.dll","jniavutil.dll");
  assertEquals(3,NativeMediaRepair.repair(directory,new ByteArrayInputStream(zip)));
  Files.delete(directory.resolve("avutil-60.dll"));Files.writeString(directory.resolve("jniavutil.dll"),"corrupt");
  assertEquals(2,NativeMediaRepair.repair(directory,new ByteArrayInputStream(zip)));
  assertEquals("avutil-60.dll-expected",Files.readString(directory.resolve("avutil-60.dll")));
  assertEquals("jniavutil.dll-expected",Files.readString(directory.resolve("jniavutil.dll")));
 }
 @Test void validCacheIsUntouched()throws Exception {
  byte[] zip=archive("version.cfg","avutil-60.dll");NativeMediaRepair.repair(directory,new ByteArrayInputStream(zip));
  var before=Files.getLastModifiedTime(directory.resolve("avutil-60.dll"));
  assertEquals(0,NativeMediaRepair.repair(directory,new ByteArrayInputStream(zip)));
  assertEquals(before,Files.getLastModifiedTime(directory.resolve("avutil-60.dll")));
 }
 @Test void invalidArchivesAreRejected()throws Exception {
  assertThrows(IOException.class,()->NativeMediaRepair.repair(directory,new ByteArrayInputStream(archive("../outside.dll"))));
  assertThrows(IOException.class,()->NativeMediaRepair.repair(directory,new ByteArrayInputStream(archive())));
  assertThrows(IOException.class,()->NativeMediaRepair.repair(directory,null));
 }
}
