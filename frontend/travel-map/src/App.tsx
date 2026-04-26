import { Switch, Route, Router as WouterRouter } from "wouter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import NotFound from "@/pages/not-found";
import MapView from "@/pages/MapView";

const queryClient = new QueryClient();
const envBase = (import.meta.env.BASE_URL || "/").replace(/\/$/, "");
const inferredBase =
  typeof window !== "undefined"
    ? window.location.pathname.replace(/\/index\.html$/, "").replace(/\/$/, "")
    : "";
const routerBase = envBase && envBase !== "/" ? envBase : inferredBase;

function Router() {
  return (
    <Switch>
      <Route path="/" component={MapView} />
      <Route component={NotFound} />
    </Switch>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <WouterRouter base={routerBase}>
          <Router />
        </WouterRouter>
        <Toaster />
      </TooltipProvider>
    </QueryClientProvider>
  );
}

export default App;
