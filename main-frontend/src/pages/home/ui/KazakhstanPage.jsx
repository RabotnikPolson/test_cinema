import { useAuth } from "@/features/auth";
import KazakhstanSection from "@/components/KazakhstanSection/KazakhstanSection";

export default function KazakhstanPage() {
  const { user } = useAuth();
  const userId = user?.profile?.userId || user?.id || null;

  return (
    <div className="page" style={{ maxWidth: 1400, margin: "0 auto", padding: "2rem" }}>
      <KazakhstanSection userId={userId} showAll />
    </div>
  );
}
