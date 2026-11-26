import { Link } from 'react-router-dom';
import { ArrowLeft, CreditCard, CheckCircle, ExternalLink } from 'lucide-react';
import { useBankLinkStatus } from '../../hooks/useFilm';
import { filmService } from '../../services/filmService';
import { useMutation, useQueryClient } from '@tanstack/react-query';

export function BankLinkPage() {
  const { data: status, isLoading } = useBankLinkStatus();
  const qc = useQueryClient();

  const linkMutation = useMutation({
    mutationFn: filmService.initiateBankLink,
    onSuccess: (data) => {
      // Redirect to Stripe onboarding
      window.location.href = data.onboardingUrl;
    },
  });

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      <header className="border-b border-neutral-800 px-6 py-4 flex items-center gap-4">
        <Link to="/owner/dashboard"
          className="flex items-center gap-2 text-neutral-400 hover:text-white transition-colors text-sm">
          <ArrowLeft className="w-4 h-4" /> Back to dashboard
        </Link>
        <h1 className="text-xl font-bold">
          Cinema<span className="text-amber-400">Book</span>
        </h1>
      </header>

      <main className="max-w-lg mx-auto px-6 py-12">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-8">

          <div className="flex items-center gap-3 mb-6">
            <div className="w-10 h-10 bg-amber-400/10 border border-amber-400/30
                            rounded-xl flex items-center justify-center">
              <CreditCard className="w-5 h-5 text-amber-400" />
            </div>
            <div>
              <h2 className="font-semibold text-lg">Bank account</h2>
              <p className="text-neutral-400 text-sm">Connect to receive ticket earnings</p>
            </div>
          </div>

          {isLoading ? (
            <p className="text-neutral-400 text-sm">Loading…</p>
          ) : status?.bankAccountLinked ? (
            <div>
              <div className="flex items-center gap-3 bg-green-900/20 border border-green-800
                              rounded-xl p-4 mb-6">
                <CheckCircle className="w-5 h-5 text-green-400 flex-shrink-0" />
                <div>
                  <p className="text-green-400 font-medium text-sm">Bank account connected</p>
                  <p className="text-neutral-400 text-xs mt-0.5">
                    Payouts are {status.payoutEnabled ? 'enabled' : 'pending verification'}
                  </p>
                </div>
              </div>
              <p className="text-neutral-400 text-sm">
                Your bank account is linked via Stripe Connect.
                Ticket earnings will be automatically transferred to your account.
              </p>
            </div>
          ) : (
            <div>
              <div className="space-y-3 mb-8">
                {[
                  'Securely connect your bank account via Stripe',
                  'Automatically receive payouts for every ticket sold',
                  'View earnings and payout history in your dashboard',
                  'Payouts processed within 2–7 business days',
                ].map((item, i) => (
                  <div key={i} className="flex items-start gap-3">
                    <div className="w-5 h-5 rounded-full bg-amber-400/20 border border-amber-400/40
                                    flex items-center justify-center flex-shrink-0 mt-0.5">
                      <span className="text-amber-400 text-xs font-bold">{i + 1}</span>
                    </div>
                    <p className="text-neutral-300 text-sm">{item}</p>
                  </div>
                ))}
              </div>

              {linkMutation.isError && (
                <div className="bg-red-950 border border-red-800 rounded-lg px-4 py-3 mb-4">
                  <p className="text-sm text-red-300">Failed to connect. Please try again.</p>
                </div>
              )}

              <button
                onClick={() => linkMutation.mutate()}
                disabled={linkMutation.isPending}
                className="w-full bg-amber-400 hover:bg-amber-300 disabled:opacity-50
                           text-neutral-950 font-semibold rounded-lg py-3 text-sm
                           transition-colors flex items-center justify-center gap-2">
                <ExternalLink className="w-4 h-4" />
                {linkMutation.isPending ? 'Connecting…' : 'Connect via Stripe →'}
              </button>

              <p className="text-xs text-neutral-500 text-center mt-4">
                You will be redirected to Stripe to securely enter your bank details.
                CinemaBook never sees your banking credentials.
              </p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}