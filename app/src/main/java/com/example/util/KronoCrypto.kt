package com.example.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KronoCrypto {
  private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
  private const val VAULT_KEY_ALIAS = "krono_vault_aes256_key"
  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val LEGACY_PIN_SALT = "KronoFiles_Vault_Salt_v1"

  fun generateSalt(): String {
    val bytes = ByteArray(16)
    java.security.SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun hashPin(pin: String, salt: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun verifyPin(pin: String, salt: String?, storedHash: String): Boolean {
    if (!salt.isNullOrBlank() && hashPin(pin, salt) == storedHash) return true
    return hashPin(pin, LEGACY_PIN_SALT) == storedHash
  }

  private fun getOrCreateVaultKey(): SecretKey {
    val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    val existing = ks.getKey(VAULT_KEY_ALIAS, null) as? SecretKey
    if (existing != null) return existing

    val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
    val spec =
      KeyGenParameterSpec.Builder(
          VAULT_KEY_ALIAS,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()
    keyGen.init(spec)
    return keyGen.generateKey()
  }

  fun encryptFile(sourceFile: File, encryptedTarget: File) {
    FileInputStream(sourceFile).use { fis ->
      encryptStream(fis, encryptedTarget)
    }
  }

  fun encryptStream(input: InputStream, encryptedTarget: File) {
    encryptedTarget.parentFile?.mkdirs()
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateVaultKey())
    val iv = cipher.iv // 12 bytes for GCM

    FileOutputStream(encryptedTarget).use { fos ->
      fos.write(iv.size)
      fos.write(iv)
      CipherOutputStream(fos, cipher).use { cos ->
        input.copyTo(cos)
      }
    }
  }

  fun decryptFile(encryptedFile: File, targetFile: File) {
    targetFile.parentFile?.mkdirs()
    FileInputStream(encryptedFile).use { fis ->
      val ivLen = fis.read()
      require(ivLen in 12..32) { "Invalid encrypted vault header" }
      val iv = ByteArray(ivLen)
      var readTotal = 0
      while (readTotal < ivLen) {
        val r = fis.read(iv, readTotal, ivLen - readTotal)
        if (r < 0) error("Corrupt vault file")
        readTotal += r
      }
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, getOrCreateVaultKey(), GCMParameterSpec(128, iv))
      CipherInputStream(fis, cipher).use { cis ->
        FileOutputStream(targetFile).use { fos -> cis.copyTo(fos) }
      }
    }
  }

  fun decryptToTempCacheFile(context: Context, encryptedFile: File, originalFileName: String): File {
    val cacheDir = File(context.cacheDir, "vault_preview").apply { mkdirs() }
    val outFile = File(cacheDir, originalFileName)
    decryptFile(encryptedFile, outFile)
    return outFile
  }

  fun clearTempPreviewCache(context: Context) {
    val cacheDir = File(context.cacheDir, "vault_preview")
    if (cacheDir.exists()) {
      runCatching { cacheDir.deleteRecursively() }
    }
  }
}
