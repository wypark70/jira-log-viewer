import React, { useState, useEffect, useRef } from 'react';
import { Plus, Edit2, Trash2, MoreVertical, Loader2, Moon, Sun, Palette, Type, Check, AlertCircle } from 'lucide-react';
import { DndContext, closestCenter, useSensor, useSensors, TouchSensor, MouseSensor } from '@dnd-kit/core';
import { arrayMove, SortableContext, useSortable, rectSortingStrategy } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

const REALISTIC_DATA = [
  { name: 'Google', bgColor: '#4285F4', textColor: '#ffffff' }, { name: 'Naver', bgColor: '#03C75A', textColor: '#ffffff' },
  { name: 'YouTube', bgColor: '#FF0000', textColor: '#ffffff' }, { name: 'GitHub', bgColor: '#181717', textColor: '#ffffff' },
  { name: 'Notion', bgColor: '#000000', textColor: '#ffffff' }, { name: 'Figma', bgColor: '#F24E1E', textColor: '#ffffff' },
  { name: 'Slack', bgColor: '#4A154B', textColor: '#ffffff' }, { name: 'Discord', bgColor: '#5865F2', textColor: '#ffffff' },
  { name: 'ChatGPT', bgColor: '#74aa9c', textColor: '#ffffff' }, { name: 'Netflix', bgColor: '#E50914', textColor: '#ffffff' },
  { name: 'Instagram', bgColor: '#E1306C', textColor: '#ffffff' }, { name: 'Twitter', bgColor: '#1DA1F2', textColor: '#ffffff' },
  { name: 'LinkedIn', bgColor: '#0A66C2', textColor: '#ffffff' }, { name: 'Spotify', bgColor: '#1DB954', textColor: '#ffffff' },
  { name: 'Amazon', bgColor: '#FF9900', textColor: '#ffffff' }, { name: 'Apple', bgColor: '#000000', textColor: '#ffffff' },
  { name: 'Trello', bgColor: '#0079BF', textColor: '#ffffff' }, { name: 'Dropbox', bgColor: '#0061FF', textColor: '#ffffff' },
  { name: 'Zoom', bgColor: '#2D8CFF', textColor: '#ffffff' }, { name: 'Reddit', bgColor: '#FF4500', textColor: '#ffffff' },
  { name: 'Kakao', bgColor: '#FEE500', textColor: '#3c1e1e' }, { name: 'Coupang', bgColor: '#cc0000', textColor: '#ffffff' },
  { name: 'Toss', bgColor: '#0050ff', textColor: '#ffffff' }, { name: 'YoutubeM', bgColor: '#ff0000', textColor: '#ffffff' },
  { name: 'Medium', bgColor: '#000000', textColor: '#ffffff' }, { name: 'Twitch', bgColor: '#9146FF', textColor: '#ffffff' },
  { name: 'Pinterest', bgColor: '#E60023', textColor: '#ffffff' }, { name: 'Webtoon', bgColor: '#00c73c', textColor: '#ffffff' },
  { name: 'Daum', bgColor: '#ff4646', textColor: '#ffffff' }, { name: 'Dribbble', bgColor: '#EA4C89', textColor: '#ffffff' },
  { name: 'Behance', bgColor: '#1769FF', textColor: '#ffffff' }, { name: 'StackO', bgColor: '#F58025', textColor: '#ffffff' },
  { name: 'CodePen', bgColor: '#000000', textColor: '#ffffff' }, { name: 'VSCode', bgColor: '#007ACC', textColor: '#ffffff' },
  { name: 'Wordpress', bgColor: '#21759B', textColor: '#ffffff' }, { name: 'Wix', bgColor: '#000000', textColor: '#ffffff' },
  { name: 'Canva', bgColor: '#00C4CC', textColor: '#ffffff' }, { name: 'Sketch', bgColor: '#F7B500', textColor: '#ffffff' },
  { name: 'Jira', bgColor: '#0052CC', textColor: '#ffffff' }, { name: 'Confluence', bgColor: '#172B4D', textColor: '#ffffff' },
  { name: 'Asana', bgColor: '#FC636B', textColor: '#ffffff' }, { name: 'ClickUp', bgColor: '#7B68EE', textColor: '#ffffff' },
  { name: 'Linear', bgColor: '#5E6AD2', textColor: '#ffffff' }, { name: 'Vercel', bgColor: '#000000', textColor: '#ffffff' },
  { name: 'Netlify', bgColor: '#00C7B7', textColor: '#ffffff' }, { name: 'Stripe', bgColor: '#635BFF', textColor: '#ffffff' },
  { name: 'PayPal', bgColor: '#003087', textColor: '#ffffff' }, { name: 'Wise', bgColor: '#8DE02C', textColor: '#ffffff' },
  { name: 'Framer', bgColor: '#000000', textColor: '#ffffff' }, { name: 'Raycast', bgColor: '#FF6363', textColor: '#ffffff' }
];

const api = {
  get: () => new Promise(res => setTimeout(() => res(
    REALISTIC_DATA.map((item, i) => ({ ...item, id: `${i + 1}`, url: `https://${item.name.toLowerCase()}.com` }))
  ), 800))
};

const SortableBookmark = ({ bm, onEdit, onDelete, activeMenuId, setActiveMenuId, darkMode }) => {
  const { attributes, listeners, setNodeRef, transform, transition } = useSortable({ id: bm.id });
  const style = { transform: CSS.Transform.toString(transform), transition };

  return (
    <div ref={setNodeRef} style={style} className="flex flex-col items-center relative group w-16 sm:w-20 m-1.5">
      <div className={`w-16 h-16 sm:w-20 sm:h-20 rounded-2xl shadow-md flex flex-col items-center justify-center font-bold text-xl mb-2 cursor-grab active:cursor-grabbing transition hover:scale-105 ring-1 ring-black/5 relative overflow-hidden ${darkMode ? 'bg-gray-800/80' : 'bg-white'}`} 
           style={{ color: bm.textColor, backgroundColor: bm.bgColor }}
           {...attributes} {...listeners}>
        <button onClick={(e) => { e.stopPropagation(); setActiveMenuId(activeMenuId === bm.id ? null : bm.id); }} 
                className="absolute top-1.5 right-1.5 p-0.5 rounded-full hover:bg-black/10 transition z-10">
          <MoreVertical size={14} />
        </button>
        {bm.name.substring(0, 3)}
      </div>
      <span className="font-semibold text-xs truncate max-w-[70px] drop-shadow-md">{bm.name}</span>

      {activeMenuId === bm.id && (
        <div className={`absolute top-8 right-0 rounded-xl shadow-2xl py-1.5 w-28 z-50 overflow-hidden ring-1 ring-black/5 animate-in fade-in zoom-in duration-200 ${darkMode ? 'bg-gray-700' : 'bg-white'}`}>
          <button onClick={() => { onEdit(bm); setActiveMenuId(null); }} className={`w-full text-left px-3 py-1.5 flex items-center gap-2 text-xs font-semibold ${darkMode ? 'hover:bg-gray-600 text-gray-200' : 'hover:bg-gray-50 text-gray-800'}`}>
            <Edit2 size={14} /> 수정
          </button>
          <button onClick={() => { onDelete(bm.id); setActiveMenuId(null); }} className={`w-full text-left px-3 py-1.5 flex items-center gap-2 text-xs font-semibold ${darkMode ? 'hover:bg-gray-600 text-red-400' : 'hover:bg-gray-50 text-red-500'}`}>
            <Trash2 size={14} /> 삭제
          </button>
        </div>
      )}
    </div>
  );
};

export default function App() {
  const [bookmarks, setBookmarks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [darkMode, setDarkMode] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [activeMenuId, setActiveMenuId] = useState(null);
  const [preview, setPreview] = useState({ bgColor: '#ffffff', textColor: '#374151', name: '', url: '' });
  const [error, setError] = useState('');

  useEffect(() => {
    api.get().then(data => { setBookmarks(data); setLoading(false); });
  }, []);

  const handleSave = () => {
    const urlRegex = /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([/\w .-]*)*\/?$/;
    if (preview.name.length < 3) {
      setError('이름은 3자 이상 입력해주세요.');
      return;
    }
    if (!urlRegex.test(preview.url)) {
      setError('올바른 웹 주소 형식이 아닙니다.');
      return;
    }
    
    if (editingId) {
      setBookmarks(prev => prev.map(b => b.id === editingId ? { ...preview, id: editingId } : b));
    } else {
      setBookmarks(prev => [...prev, { ...preview, id: Date.now().toString() }]);
    }
    setError('');
    setIsModalOpen(false);
  };

  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 5 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 200, tolerance: 5 } })
  );

  return (
    <div className={`h-screen flex flex-col transition-all duration-700 overflow-hidden ${darkMode ? 'bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-950 via-gray-900 to-purple-950 text-white' : 'bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 text-white'}`}>
      <div className="flex-1 overflow-y-auto px-4 py-8" onClick={() => setActiveMenuId(null)}>
        {loading ? (
          <div className="h-full flex items-center justify-center">
            <Loader2 className="animate-spin text-white" size={48} />
          </div>
        ) : (
          <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={(e) => {
            const { active, over } = e;
            if (over && active.id !== over.id) {
              setBookmarks(items => {
                const oldIndex = items.findIndex(i => i.id === active.id);
                const newIndex = items.findIndex(i => i.id === over.id);
                return arrayMove(items, oldIndex, newIndex);
              });
            }
          }}>
            <div className="flex flex-wrap justify-center gap-1 max-w-5xl mx-auto">
              <SortableContext items={bookmarks} strategy={rectSortingStrategy}>
                {bookmarks.map(bm => (
                  <SortableBookmark key={bm.id} bm={bm} activeMenuId={activeMenuId} setActiveMenuId={setActiveMenuId} darkMode={darkMode} 
                    onDelete={(id) => setBookmarks(bookmarks.filter(b => b.id !== id))} 
                    onEdit={(item) => { setEditingId(item.id); setPreview(item); setIsModalOpen(true); }} 
                  />
                ))}
              </SortableContext>
              <button onClick={() => { setEditingId(null); setPreview({bgColor: '#ffffff', textColor: '#374151', name: '', url: ''}); setIsModalOpen(true); }} className="flex flex-col items-center group m-1.5">
                <div className={`w-16 h-16 sm:w-20 sm:h-20 rounded-2xl flex items-center justify-center backdrop-blur-md border border-white/10 shadow-md transition ${darkMode ? 'bg-gray-800/60' : 'bg-white/10'}`}>
                  <Plus size={32} />
                </div>
                <span className="mt-2 font-semibold text-xs">추가</span>
              </button>
            </div>
          </DndContext>
        )}
      </div>

      <button onClick={() => setDarkMode(!darkMode)} className="fixed bottom-6 right-6 p-4 rounded-full bg-black/30 hover:bg-black/50 backdrop-blur-md transition-all z-50 text-white shadow-xl">
        {darkMode ? <Sun size={24} /> : <Moon size={24} />}
      </button>

      {isModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4" onClick={() => setIsModalOpen(false)}>
          <div className={`${darkMode ? 'bg-gray-800 text-white' : 'bg-white text-gray-800'} w-full max-w-xs rounded-2xl p-3 shadow-2xl transition-colors`} onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-2 mb-2">
              <div className="w-12 h-12 rounded-xl flex items-center justify-center font-bold text-lg shadow-inner border border-black/5 shrink-0 relative" 
                   style={{ backgroundColor: preview.bgColor, color: preview.textColor }}>
                {preview.name.substring(0, 1) || 'A'}
                <div className="absolute -top-1 -right-1 flex flex-col gap-0.5">
                  <label className="bg-white p-0.5 rounded-full shadow border cursor-pointer"><Palette size={9} className="text-gray-700" /><input type="color" className="hidden" onChange={(e) => setPreview({...preview, bgColor: e.target.value})} /></label>
                  <label className="bg-white p-0.5 rounded-full shadow border cursor-pointer"><Type size={9} className="text-gray-700" /><input type="color" className="hidden" onChange={(e) => setPreview({...preview, textColor: e.target.value})} /></label>
                </div>
              </div>
              
              <div className="flex-1 space-y-1">
                <input placeholder="이름 (3자 이상)" value={preview.name} onChange={(e) => setPreview({...preview, name: e.target.value})} className={`w-full px-2 py-1 text-xs rounded-md ${darkMode ? 'bg-gray-700 text-white' : 'bg-gray-100 text-gray-800'}`} />
                <input placeholder="URL" value={preview.url} onChange={(e) => setPreview({...preview, url: e.target.value})} className={`w-full px-2 py-1 text-xs rounded-md ${darkMode ? 'bg-gray-700 text-white' : 'bg-gray-100 text-gray-800'}`} />
              </div>
              
              <button onClick={handleSave} className="h-12 px-2.5 bg-indigo-600 text-white rounded-lg flex items-center justify-center"><Check size={18}/></button>
            </div>
            {error && <div className="text-red-500 text-xs flex items-center gap-1"><AlertCircle size={12}/> {error}</div>}
          </div>
        </div>
      )}
    </div>
  );
}


