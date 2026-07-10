//! CrabMagick's bundled pure-Rust image processing core.

#![recursion_limit = "256"]

extern crate alloc;

whereat::define_at_crate_info!(path = "crabmagick-core/");

/// WebP decode implementation.
pub(crate) mod webp_decode;
/// JXL encode implementation.
pub mod jxl_encode;
/// JXL decode implementation (AVX2/AVX512 DCT).
pub(crate) mod jxl_decode;
/// JXL encoder SIMD kernels.
pub(crate) mod jxl_encode_simd;
/// Low-level decode, transform, and encode primitives.
pub mod pipeline;
/// High-level request types and orchestration helpers.
pub mod processor;
/// JPEG 2000 decode implementation.
pub(crate) mod jpeg2000_decode;
/// JPEG encode implementation.
pub(crate) mod jpeg_encode;
/// JPEG decoder core types.
pub(crate) mod jpeg_decode_core;
/// JPEG decode implementation (SIMD AVX-512/AVX2/NEON).
pub(crate) mod jpeg_decode;

pub use pipeline::{JxlEncodeOptions, decode_jxl_info_from_bytes, encode_jxl_rgb};

/// Alias for vendored jxl_oxide module, available in tests under the original crate name.
#[cfg(test)]
pub(crate) mod jxl_oxide {
    pub use crate::jxl_decode::jxl_oxide::*;
}
pub use jxl_encode::{EncoderMode, PixelLayout as JxlPixelLayout};
pub use processor::{
    get_info, init, process_image, CrabMagickError, CrabMagickProcessor, ImageInfo, OutputFormat,
    ProcessRequest, RequestedRegion,
};

#[unsafe(no_mangle)]
pub unsafe extern "C" fn process(
    input: *const u8,
    input_len: usize,
    output: *mut *mut u8,
    output_len: *mut usize,
) -> i32 {
    if input.is_null() || output.is_null() || output_len.is_null() {
        return -1;
    }
    let input_slice = unsafe { std::slice::from_raw_parts(input, input_len) };

    let temp_path = match write_temp_file(input_slice) {
        Ok(path) => path,
        Err(_) => return -4,
    };

    let request = ProcessRequest::with_quality(OutputFormat::Jxl, 85);
    let path_str = match temp_path.to_str() {
        Some(s) => s,
        None => {
            let _ = std::fs::remove_file(&temp_path);
            return -5;
        }
    };

    let result = process_image(path_str, request);
    let _ = std::fs::remove_file(&temp_path);

    match result {
        Ok(out_bytes) => {
            let out_len = out_bytes.len();
            let ptr = unsafe { libc::malloc(out_len) };
            if ptr.is_null() {
                return -3;
            }
            unsafe {
                std::ptr::copy_nonoverlapping(out_bytes.as_ptr(), ptr as *mut u8, out_len);
                *output = ptr as *mut u8;
                *output_len = out_len;
            }
            0
        }
        Err(_) => -2,
    }
}

fn write_temp_file(bytes: &[u8]) -> std::io::Result<std::path::PathBuf> {
    use std::fs::File;
    use std::io::Write;
    use std::path::PathBuf;

    let temp_dir = std::env::var("TMPDIR")
        .map(PathBuf::from)
        .unwrap_or_else(|_| {
            let p = PathBuf::from("/data/local/tmp");
            if p.exists() {
                p
            } else {
                PathBuf::from(".")
            }
        });
    let num = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_nanos())
        .unwrap_or(0);
    let path = temp_dir.join(format!("crabmagick_temp_{num}.tmp"));
    let mut file = File::create(&path)?;
    file.write_all(bytes)?;
    Ok(path)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_github_anaruto_libcrabmagick_CrabMagick_nativeApplyMagic<'local>(
    mut env: jni::JNIEnv<'local>,
    _class: jni::objects::JClass<'local>,
    input_array: jni::objects::JByteArray<'local>,
) -> jni::sys::jbyteArray {
    let mut handle_error = |env: &mut jni::JNIEnv, msg: &str| -> jni::sys::jbyteArray {
        let _ = env.throw_new("java/lang/RuntimeException", msg);
        std::ptr::null_mut()
    };

    let input_vec = match env.convert_byte_array(&input_array) {
        Ok(v) => v,
        Err(_) => return handle_error(&mut env, "Failed to read input array"),
    };

    let temp_path = match write_temp_file(&input_vec) {
        Ok(path) => path,
        Err(_) => return handle_error(&mut env, "Failed to write temp file"),
    };

    let request = ProcessRequest::with_quality(OutputFormat::Jxl, 85);
    let path_str = match temp_path.to_str() {
        Some(s) => s,
        None => {
            let _ = std::fs::remove_file(&temp_path);
            return handle_error(&mut env, "Invalid temp path");
        }
    };

    let result = process_image(path_str, request);
    let _ = std::fs::remove_file(&temp_path);

    match result {
        Ok(out_bytes) => {
            match env.byte_array_from_slice(&out_bytes) {
                Ok(arr) => arr.into_raw(),
                Err(_) => handle_error(&mut env, "Failed to allocate output JByteArray"),
            }
        }
        Err(_) => handle_error(&mut env, "crabmagick processing failed"),
    }
}

