import { useEffect, useRef, useState, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ArrowLeft, Scissors, AlertCircle, CheckCircle } from 'lucide-react';
import { watcherService } from '../../services/watcherService';

const PROGRESS_REPORT_INTERVAL = 15_000; // send progress every 15 seconds

export function WatchPage() {
  const { filmId } = useParams<{ filmId: string }>();
  const navigate  = useNavigate();
  const videoRef  = useRef<HTMLVideoElement>(null);

  const [sessionError, setSessionError] = useState('');
  const [clipStart,    setClipStart]    = useState(0);
  const [clipDuration, setClipDuration] = useState(60);
  const [clipMsg,      setClipMsg]      = useState('');
  const [showClipPanel, setShowClipPanel] = useState(false);

  // Fetch the ticket first to check entitlement
  const { data: tickets } = useQuery({
    queryKey: ['myTickets', 0],
    queryFn: () => watcherService.getMyTickets(0, 100),
  });

  const ticket = tickets?.tickets.find(t => t.filmId === filmId);

  // Start stream session
  const { data: session, isLoading: sessionLoading, error: sessionErr } = useQuery({
    queryKey: ['streamSession', filmId],
    queryFn: () => watcherService.startStream(filmId!),
    enabled: !!filmId && !!ticket,
    staleTime: 30 * 60 * 1000,   // reuse for 30 min before re-fetching
    retry: 1,
  });

  // Set video src and resume position when session is ready
  useEffect(() => {
    if (session && videoRef.current) {
      videoRef.current.src = session.streamUrl;
      videoRef.current.currentTime = session.resumePositionSec;
    }
  }, [session]);

  // Progress reporting — sends position every 15 seconds while playing
  const lastReportedPosition = useRef(0);
  const progressInterval = useRef<ReturnType<typeof setInterval>>();

  const reportProgress = useCallback(() => {
    if (!videoRef.current || !ticket) return;
    const pos = Math.floor(videoRef.current.currentTime);
    if (pos === lastReportedPosition.current) return;
    lastReportedPosition.current = pos;
    watcherService.updateProgress(ticket.id, pos, 'PROGRESS').catch(() => {});
  }, [ticket]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !ticket) return;

    const onPlay  = () => {
      watcherService.updateProgress(ticket.id, Math.floor(video.currentTime), 'START');
      progressInterval.current = setInterval(reportProgress, PROGRESS_REPORT_INTERVAL);
    };
    const onPause = () => {
      clearInterval(progressInterval.current);
      watcherService.updateProgress(ticket.id, Math.floor(video.currentTime), 'PAUSE');
    };
    const onEnded = () => {
      clearInterval(progressInterval.current);
      watcherService.updateProgress(ticket.id, Math.floor(video.currentTime), 'COMPLETE');
    };

    video.addEventListener('play',  onPlay);
    video.addEventListener('pause', onPause);
    video.addEventListener('ended', onEnded);

    return () => {
      clearInterval(progressInterval.current);
      video.removeEventListener('play',  onPlay);
      video.removeEventListener('pause', onPause);
      video.removeEventListener('ended', onEnded);
    };
  }, [ticket, reportProgress]);

  // Clip request
  const clipMutation = useMutation({
    mutationFn: () => watcherService.requestClip(filmId!, {
      startSeconds:    clipStart,
      durationSeconds: clipDuration,
    }),
    onSuccess: (job) => {
      setClipMsg(job.status === 'DONE'
        ? '✓ Clip created! Check your clip history.'
        : 'Clip is being processed…');
      setShowClipPanel(false);
    },
    onError: (err: any) => {
      setClipMsg(err?.response?.data?.error ?? 'Clip request failed.');
    },
  });

  // Redirect if no ticket
  useEffect(() => {
    if (tickets && !ticket) {
      navigate(`/films/${filmId}`);
    }
  }, [tickets, ticket, filmId, navigate]);

  return (
    <div className="min-h-screen bg-black text-white flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-3 bg-neutral-950 border-b border-neutral-900">
        <Link to="/dashboard"
          className="flex items-center gap-2 text-neutral-400 hover:text-white transition-colors text-sm">
          <ArrowLeft className="w-4 h-4" /> Back
        </Link>
        {session && (
          <h2 className="text-sm font-medium text-neutral-300">{session.filmTitle}</h2>
        )}
        <button onClick={() => setShowClipPanel(p => !p)}
          className="flex items-center gap-2 text-sm bg-neutral-800 hover:bg-neutral-700
                     px-3 py-1.5 rounded-lg transition-colors">
          <Scissors className="w-4 h-4 text-amber-400" />
          Clip ({ticket?.clipSecondsRemaining ?? 0}s left)
        </button>
      </div>

      {/* Player area */}
      <div className="flex-1 flex items-center justify-center bg-black relative">
        {sessionLoading && (
          <div className="flex flex-col items-center gap-3 text-neutral-400">
            <div className="w-8 h-8 border-2 border-amber-400 border-t-transparent rounded-full animate-spin" />
            <p className="text-sm">Starting stream…</p>
          </div>
        )}

        {(sessionErr || sessionError) && (
          <div className="flex flex-col items-center gap-3 text-center max-w-md px-6">
            <AlertCircle className="w-10 h-10 text-red-400" />
            <p className="text-neutral-300">Could not start stream. You may not have a valid ticket.</p>
            <Link to="/dashboard" className="text-amber-400 hover:underline text-sm">
              ← Back to dashboard
            </Link>
          </div>
        )}

        {session && (
          <video
            ref={videoRef}
            className="w-full max-h-[80vh]"
            controls
            controlsList="nodownload nofullscreen noremoteplayback"  // disables download button
            disablePictureInPicture                                    // blocks PiP capture
            playsInline
            onContextMenu={e => e.preventDefault()}                   // disables right-click
          />
        )}
      </div>

      {/* Clip panel */}
      {showClipPanel && (
        <div className="bg-neutral-950 border-t border-neutral-800 px-6 py-5">
          <h3 className="font-semibold mb-4 flex items-center gap-2">
            <Scissors className="w-4 h-4 text-amber-400" />
            Request a clip — up to 5 minutes total per ticket
          </h3>

          <div className="flex flex-wrap items-end gap-4">
            <div>
              <label className="block text-xs text-neutral-400 mb-1">Start (seconds)</label>
              <input type="number" min={0} value={clipStart}
                onChange={e => setClipStart(Number(e.target.value))}
                className="bg-neutral-800 border border-neutral-700 rounded-lg px-3 py-2
                           text-sm w-32 focus:outline-none focus:border-amber-400" />
            </div>
            <div>
              <label className="block text-xs text-neutral-400 mb-1">Duration (1–300s)</label>
              <input type="number" min={1} max={Math.min(300, ticket?.clipSecondsRemaining ?? 300)}
                value={clipDuration}
                onChange={e => setClipDuration(Number(e.target.value))}
                className="bg-neutral-800 border border-neutral-700 rounded-lg px-3 py-2
                           text-sm w-32 focus:outline-none focus:border-amber-400" />
            </div>
            <button
              onClick={() => {
                setClipMsg('');
                if (videoRef.current) {
                  setClipStart(Math.floor(videoRef.current.currentTime));
                }
                clipMutation.mutate();
              }}
              disabled={clipMutation.isPending || !ticket || ticket.clipSecondsRemaining === 0}
              className="bg-amber-400 hover:bg-amber-300 disabled:opacity-50 text-neutral-950
                         font-semibold px-4 py-2 rounded-lg text-sm transition-colors flex items-center gap-2">
              <Scissors className="w-4 h-4" />
              {clipMutation.isPending ? 'Creating…' : 'Create clip'}
            </button>
          </div>

          {clipMsg && (
            <div className={`flex items-center gap-2 mt-3 text-sm
              ${clipMsg.startsWith('✓') ? 'text-green-400' : 'text-red-400'}`}>
              {clipMsg.startsWith('✓')
                ? <CheckCircle className="w-4 h-4" />
                : <AlertCircle className="w-4 h-4" />}
              {clipMsg}
            </div>
          )}

          <p className="text-xs text-neutral-500 mt-3">
            Clips are watermarked with your user ID and created server-side.
            They can be shared in CinemaBook chats and posts.
          </p>
        </div>
      )}
    </div>
  );
}