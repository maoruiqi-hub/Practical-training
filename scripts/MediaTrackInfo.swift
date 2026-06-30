import AVFoundation
import Foundation

if CommandLine.arguments.count < 2 {
    fputs("Usage: MediaTrackInfo <media-file>\n", stderr)
    exit(1)
}

let asset = AVURLAsset(url: URL(fileURLWithPath: CommandLine.arguments[1]))
let videoCount = asset.tracks(withMediaType: .video).count
let audioCount = asset.tracks(withMediaType: .audio).count
let seconds = CMTimeGetSeconds(asset.duration)
print("videoTracks=\(videoCount) audioTracks=\(audioCount) duration=\(String(format: "%.2f", seconds))")
