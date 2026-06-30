import AVFoundation
import AppKit
import Foundation

func fail(_ message: String) -> Never {
    fputs(message + "\n", stderr)
    exit(1)
}

if CommandLine.arguments.count < 4 {
    fail("Usage: VideoBuilder <frames-dir> <output-mp4> <fps>")
}

let framesDir = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
let outputURL = URL(fileURLWithPath: CommandLine.arguments[2])
let fps = Int32(CommandLine.arguments[3]) ?? 1
let width = 1280
let height = 720

let fm = FileManager.default
let directoryContents = (try? fm.contentsOfDirectory(at: framesDir, includingPropertiesForKeys: nil)) ?? []
let frameURLs = directoryContents
    .filter { $0.pathExtension.lowercased() == "png" }
    .sorted { $0.lastPathComponent < $1.lastPathComponent }

if frameURLs.isEmpty {
    fail("No PNG frames found in \(framesDir.path)")
}

try? fm.removeItem(at: outputURL)

guard let writer = try? AVAssetWriter(outputURL: outputURL, fileType: .mp4) else {
    fail("Cannot create AVAssetWriter")
}

let settings: [String: Any] = [
    AVVideoCodecKey: AVVideoCodecType.h264,
    AVVideoWidthKey: width,
    AVVideoHeightKey: height
]

let input = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
input.expectsMediaDataInRealTime = false

let attributes: [String: Any] = [
    kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB,
    kCVPixelBufferWidthKey as String: width,
    kCVPixelBufferHeightKey as String: height
]

let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: attributes)

guard writer.canAdd(input) else {
    fail("Cannot add writer input")
}
writer.add(input)

guard writer.startWriting() else {
    fail("Cannot start writing: \(writer.error?.localizedDescription ?? "unknown error")")
}
writer.startSession(atSourceTime: .zero)

func pixelBuffer(from imageURL: URL) -> CVPixelBuffer? {
    guard let image = NSImage(contentsOf: imageURL),
          let cgImage = image.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
        return nil
    }

    var buffer: CVPixelBuffer?
    let status = CVPixelBufferPoolCreatePixelBuffer(nil, adaptor.pixelBufferPool!, &buffer)
    guard status == kCVReturnSuccess, let pixelBuffer = buffer else {
        return nil
    }

    CVPixelBufferLockBaseAddress(pixelBuffer, [])
    defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, []) }

    guard let context = CGContext(
        data: CVPixelBufferGetBaseAddress(pixelBuffer),
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: CVPixelBufferGetBytesPerRow(pixelBuffer),
        space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
    ) else {
        return nil
    }

    context.clear(CGRect(x: 0, y: 0, width: width, height: height))
    context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
    return pixelBuffer
}

let frameDuration = CMTime(value: 1, timescale: fps)
var frameIndex: Int64 = 0
let queue = DispatchQueue(label: "video-builder")
let group = DispatchGroup()
group.enter()

input.requestMediaDataWhenReady(on: queue) {
    while input.isReadyForMoreMediaData && frameIndex < Int64(frameURLs.count) {
        let url = frameURLs[Int(frameIndex)]
        guard let buffer = pixelBuffer(from: url) else {
            input.markAsFinished()
            writer.cancelWriting()
            fail("Cannot read frame: \(url.path)")
        }
        let presentationTime = CMTimeMultiply(frameDuration, multiplier: Int32(frameIndex))
        if !adaptor.append(buffer, withPresentationTime: presentationTime) {
            input.markAsFinished()
            writer.cancelWriting()
            fail("Cannot append frame: \(writer.error?.localizedDescription ?? "unknown error")")
        }
        frameIndex += 1
    }

    if frameIndex >= Int64(frameURLs.count) {
        input.markAsFinished()
        writer.finishWriting {
            if writer.status == .completed {
                group.leave()
            } else {
                fail("Writer failed: \(writer.error?.localizedDescription ?? "unknown error")")
            }
        }
    }
}

group.wait()
