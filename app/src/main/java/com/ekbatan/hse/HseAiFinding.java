package com.ekbatan.hse;

import java.util.ArrayList;
import java.util.Locale;

/** Structured HSE finding normalized from the local multimodal model output. */
public final class HseAiFinding {
    public String category="";
    public String evidence="";
    public String certainty="";
    public String hazard="";
    public String consequence="";
    public String immediateAction="";
    public String correctiveAction="";
    public String priority="";
    public String confidence="";

    public boolean usable(){ return !evidence.trim().isEmpty() || !hazard.trim().isEmpty(); }
    public String title(){ return hazard.trim().isEmpty() ? (category.trim().isEmpty()?"یافته HSE":category.trim()) : hazard.trim(); }

    public static ArrayList<HseAiFinding> parse(String raw){
        ArrayList<HseAiFinding> out=new ArrayList<>();
        if(raw==null||raw.trim().isEmpty()) return out;
        String text=raw.replace("\r","");
        String[] blocks=text.split("(?m)(?=^\\s*(?:[-•*]?\\s*)?(?:دسته HSE|دسته|Category)\\s*[:：])");
        for(String block:blocks){ HseAiFinding f=parseBlock(block); if(f.usable()) out.add(f); }
        if(out.isEmpty()){
            HseAiFinding f=new HseAiFinding();
            String hp=firstNonEmpty(lineAfter(text,"خطر و پیامد"),"");
            if(!hp.isEmpty()){
                String[] parts=splitHazardConsequence(hp); f.hazard=parts[0]; f.consequence=parts[1];
            } else {
                f.hazard=firstNonEmpty(lineAfter(text,"خطر"),lineAfter(text,"یافته"),"یافته نیازمند بررسی");
                f.consequence=lineAfter(text,"پیامد");
            }
            f.evidence=firstNonEmpty(lineAfter(text,"شاهد قابل مشاهده"),lineAfter(text,"شاهد"),text.trim());
            f.correctiveAction=firstNonEmpty(lineAfter(text,"اقدام اصلاحی پیشنهادی"),lineAfter(text,"اقدام اصلاحی"),"");
            f.immediateAction=firstNonEmpty(lineAfter(text,"اقدام فوری"),"");
            f.priority=firstNonEmpty(lineAfter(text,"اولویت"),"نیازمند بازدید");
            f.confidence=firstNonEmpty(lineAfter(text,"اطمینان"),"پایین");
            if(f.usable()) out.add(f);
        }
        return out;
    }
    private static HseAiFinding parseBlock(String b){
        HseAiFinding f=new HseAiFinding();
        f.category=value(b,"دسته HSE"); if(f.category.isEmpty()) f.category=value(b,"دسته");
        f.evidence=value(b,"شاهد قابل مشاهده"); if(f.evidence.isEmpty()) f.evidence=value(b,"شاهد");
        f.certainty=value(b,"وضعیت");
        String hp=value(b,"خطر و پیامد");
        if(!hp.isEmpty()){
            String[] parts=splitHazardConsequence(hp);
            f.hazard=parts[0]; f.consequence=parts[1];
        } else {
            f.hazard=value(b,"خطر");
            f.consequence=value(b,"پیامد");
        }
        f.immediateAction=value(b,"اقدام فوری"); f.correctiveAction=value(b,"اقدام اصلاحی پیشنهادی"); if(f.correctiveAction.isEmpty()) f.correctiveAction=value(b,"اقدام اصلاحی");
        f.priority=value(b,"اولویت"); f.confidence=value(b,"اطمینان"); return f;
    }
    private static String[] splitHazardConsequence(String value){
        String v=value==null?"":value.trim();
        String[] markers={"پیامد:","پیامد：","پیامد"};
        for(String marker:markers){
            int p=normalize(v).indexOf(normalize(marker));
            if(p>=0){
                String rawMarker=marker;
                int raw=v.indexOf(rawMarker);
                if(raw>=0){
                    String h=v.substring(0,raw).trim();
                    String c=v.substring(raw+rawMarker.length()).trim();
                    if(h.endsWith("-")||h.endsWith("—")||h.endsWith(":")) h=h.substring(0,h.length()-1).trim();
                    return new String[]{h,c};
                }
            }
        }
        return new String[]{v,""};
    }

    private static String value(String b,String key){
        String[] lines=b.split("\n");
        for(String l:lines){String n=l.trim();int p=n.indexOf(":");if(p<0)p=n.indexOf("：");if(p>=0){String k=n.substring(0,p).replace("-"," ").replace("•"," ").replace("*"," ").trim();if(normalize(k).contains(normalize(key)))return n.substring(p+1).trim();}}
        return "";
    }
    private static String lineAfter(String t,String key){String[] ls=t.split("\n");for(String l:ls){if(normalize(l).contains(normalize(key))){int p=l.indexOf(":");if(p<0)p=l.indexOf("：");return p>=0?l.substring(p+1).trim():l.trim();}}return "";}
    private static String firstNonEmpty(String... a){for(String s:a)if(s!=null&&!s.trim().isEmpty())return s.trim();return "";}
    private static String normalize(String s){return s==null?"":s.replace('ي','ی').replace('ك','ک').toLowerCase(Locale.ROOT).replaceAll("\\s+","");}
}
