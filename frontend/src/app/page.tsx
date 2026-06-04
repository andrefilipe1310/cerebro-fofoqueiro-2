// @path frontend/src/app/page.tsx
// @owner frontend
// @responsibility Root page — redireciona para /cameras (dashboard)
import { redirect } from 'next/navigation';

export default function RootPage() {
  redirect('/cameras');
}
