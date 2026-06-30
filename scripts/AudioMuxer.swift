import AVFoundation
import Foundation

func fail(_ message: String) -> Never {
    fputs(message + "\n", stderr)
    exit(1)
}

if CommandLine.arguments.count < 4 {
    fail("Usage: AudioMuxer <input-video> <input-audio> <output-video>")
}

let videoURL = URL(fileURLWithPath: CommandLine.arguments[1])
let audioURL = URL(fileURLWithPath: CommandLine.arguments[2])
let outputURL = URL(fileURLWithPath: CommandLine.arguments[3])
let fm = FileManager.default

try? fm.removeItem(at: outputURL)

let videoAsset = AVURLAsset(url: videoURL)
let audioAsset = AVURLAsset(url: audioURL)
let composition = AVMutableComposition()

guard let sourceVideoTrack = videoAsset.tracks(withMediaType: .video).first else {
    fail("Input video has no video track: \(videoURL.path)")
}
guard let compositionVideoTrack = composition.addMutableTrack(
    withMediaType: .video,
    preferredTrackID: kCMPersistentTrackID_Invalid
) else {
    fail("Cannot create composition video track")
}

do {
    try compositionVideoTrack.insertTimeRange(
        CMTimeRange(start: .zero, duration: videoAsset.duration),
        of: sourceVideoTrack,
        at: .zero
    )
    compositionVideoTrack.preferredTransform = sourceVideoTrack.preferredTransform
} catch {
    fail("Cannot insert video track: \(error.localizedDescription)")
}

if let sourceAudioTrack = audioAsset.tracks(withMediaType: .audio).first,
   let compositionAudioTrack = composition.addMutableTrack(
       withMediaType: .audio,
       preferredTrackID: kCMPersistentTrackID_Invalid
   ) {
    let audioDuration = CMTimeMinimum(audioAsset.duration, videoAsset.duration)
    do {
        try compositionAudioTrack.insertTimeRange(
            CMTimeRange(start: .zero, duration: audioDuration),
            of: sourceAudioTrack,
            at: .zero
        )
    } catch {
        fail("Cannot insert audio track: \(error.localizedDescription)")
    }
} else {
    fail("Input audio has no audio track: \(audioURL.path)")
}

guard let export = AVAssetExportSession(asset: composition, presetName: AVAssetExportPresetHighestQuality) else {
    fail("Cannot create AVAssetExportSession")
}

export.outputURL = outputURL
export.outputFileType = .mp4
export.shouldOptimizeForNetworkUse = true

let semaphore = DispatchSemaphore(value: 0)
export.exportAsynchronously {
    semaphore.signal()
}
semaphore.wait()

if export.status != .completed {
    fail("Export failed: \(export.error?.localizedDescription ?? "unknown error")")
}
